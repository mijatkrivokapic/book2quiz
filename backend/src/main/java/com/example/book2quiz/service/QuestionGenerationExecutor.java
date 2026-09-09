package com.example.book2quiz.service;

import com.example.book2quiz.config.QuizProperties;
import com.example.book2quiz.dto.quiz.GeneratedQuestion;
import com.example.book2quiz.dto.quiz.GeneratedQuiz;
import com.example.book2quiz.dto.quiz.LearningObjectiveInput;
import com.example.book2quiz.dto.quiz.QuizGenerationRequest;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.GenerationKind;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.model.Question;
import com.example.book2quiz.model.QuestionOrigin;
import com.example.book2quiz.model.QuestionStatus;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.ConstraintRepository;
import com.example.book2quiz.repository.LearningObjectiveRepository;
import com.example.book2quiz.repository.QuestionRepository;
import com.example.book2quiz.repository.StructuralCharacteristicRepository;
import com.example.book2quiz.repository.SurfaceCharacteristicRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Runs quiz generation on the dedicated {@code quizExecutor}. Assembles the chapter's
 * material, calls the (synchronous, minutes-long) generator, persists the questions as
 * GENERATED / PENDING, and records the outcome on the chapter's
 * {@code questionGenerationStatus} so the UI can poll for progress. A failure is captured
 * on the chapter rather than propagated.
 */
@Component
public class QuestionGenerationExecutor {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationExecutor.class);

    private final ChapterRepository chapterRepository;
    private final QuestionRepository questionRepository;
    private final LearningObjectiveRepository learningObjectiveRepository;
    private final StructuralCharacteristicRepository structuralRepository;
    private final SurfaceCharacteristicRepository surfaceRepository;
    private final ConstraintRepository constraintRepository;
    private final FileStorageService fileStorageService;
    private final QuizGenerationService quizGenerationService;
    private final QuestionVersionManager versionManager;
    private final GenerationRecordService generationRecordService;
    private final QuizProperties quizProperties;
    private final ObjectMapper objectMapper;

    public QuestionGenerationExecutor(ChapterRepository chapterRepository,
                                      QuestionRepository questionRepository,
                                      LearningObjectiveRepository learningObjectiveRepository,
                                      StructuralCharacteristicRepository structuralRepository,
                                      SurfaceCharacteristicRepository surfaceRepository,
                                      ConstraintRepository constraintRepository,
                                      FileStorageService fileStorageService,
                                      QuizGenerationService quizGenerationService,
                                      QuestionVersionManager versionManager,
                                      GenerationRecordService generationRecordService,
                                      QuizProperties quizProperties,
                                      ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.questionRepository = questionRepository;
        this.learningObjectiveRepository = learningObjectiveRepository;
        this.structuralRepository = structuralRepository;
        this.surfaceRepository = surfaceRepository;
        this.constraintRepository = constraintRepository;
        this.fileStorageService = fileStorageService;
        this.quizGenerationService = quizGenerationService;
        this.versionManager = versionManager;
        this.generationRecordService = generationRecordService;
        this.quizProperties = quizProperties;
        this.objectMapper = objectMapper;
    }

    @Async("quizExecutor")
    public void runGeneration(int bookId, int ordinal) {
        Chapter chapter = chapterRepository.findByBookIdAndOrdinal(bookId, ordinal).orElse(null);
        if (chapter == null) {
            log.warn("Book {} chapter {}: vanished before question generation", bookId, ordinal);
            return;
        }
        int chapterId = chapter.getId();

        try {
            String material = new String(
                    fileStorageService.downloadFile(chapter.getMarkdownObjectKey()), StandardCharsets.UTF_8);
            List<LearningObjectiveInput> learningObjectives = loadLearningObjectives(chapterId);
            List<String> surface = surfaceRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> c.getContent()).toList();
            List<String> constraints = constraintRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> c.getContent()).toList();

            int structuralCount = learningObjectives.stream()
                    .mapToInt(lo -> lo.structuralCharacteristicsOrEmpty().size()).sum();
            log.info("Book {} chapter {}: generating questions ({} chars material, {} objectives, {} structural, {} surface, {} constraints)",
                    bookId, ordinal, material.length(), learningObjectives.size(), structuralCount, surface.size(), constraints.size());

            QuizGenerationRequest request =
                    new QuizGenerationRequest(List.of(material), constraints, learningObjectives, surface);

            long start = System.currentTimeMillis();
            GeneratedQuiz quiz = quizGenerationService.generate(request);
            long durationMs = System.currentTimeMillis() - start;

            persist(chapterId, quiz.questions());
            generationRecordService.record(chapterId, GenerationKind.QUESTIONS, quiz.usage(),
                    quizProperties.getPromptVersion(), durationMs, quiz.questions().size() + " questions");
            finish(chapterId, ProcessingStatus.DONE, null);
            log.info("Book {} chapter {}: generated {} question(s)", bookId, ordinal, quiz.questions().size());
        } catch (Exception e) {
            log.error("Book {} chapter {}: question generation failed", bookId, ordinal, e);
            finish(chapterId, ProcessingStatus.FAILED, truncate(e.getMessage()));
        }
    }

    /**
     * Loads the chapter's learning objectives (ordered), each paired with its ordered
     * structural characteristics, for sending into the prompt.
     */
    private List<LearningObjectiveInput> loadLearningObjectives(int chapterId) {
        return learningObjectiveRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                .map(lo -> new LearningObjectiveInput(
                        lo.getDescription(),
                        structuralRepository.findByLearningObjectiveIdOrderByIdAsc(lo.getId()).stream()
                                .map(c -> c.getContent())
                                .toList()))
                .toList();
    }

    private void persist(int chapterId, List<GeneratedQuestion> payloads) {
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow();
        for (GeneratedQuestion payload : payloads) {
            Question question = new Question();
            question.setChapter(chapter);
            question.setQuestionType(payload.questionType());
            question.setContent(serialize(payload));
            question.setOrigin(QuestionOrigin.GENERATED);
            question.setStatus(QuestionStatus.PENDING);
            question = questionRepository.save(question);
            versionManager.createInitialVersion(question);
        }
    }

    private void finish(int chapterId, ProcessingStatus status, String error) {
        chapterRepository.findById(chapterId).ifPresent(chapter -> {
            chapter.setQuestionGenerationStatus(status);
            chapter.setQuestionGenerationError(error);
            chapterRepository.save(chapter);
        });
    }

    private String serialize(GeneratedQuestion payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize question", e);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
