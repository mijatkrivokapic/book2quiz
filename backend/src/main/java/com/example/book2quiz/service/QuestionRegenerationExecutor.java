package com.example.book2quiz.service;

import com.example.book2quiz.dto.quiz.GeneratedQuestion;
import com.example.book2quiz.dto.quiz.LearningObjectiveInput;
import com.example.book2quiz.dto.quiz.QuestionRegenerationRequest;
import com.example.book2quiz.dto.quiz.RegeneratedQuestion;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.model.Question;
import com.example.book2quiz.model.QuestionOrigin;
import com.example.book2quiz.model.QuestionStatus;
import com.example.book2quiz.model.QuestionVersion;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.ConstraintRepository;
import com.example.book2quiz.repository.LearningObjectiveRepository;
import com.example.book2quiz.repository.QuestionRepository;
import com.example.book2quiz.repository.QuestionVersionRepository;
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
 * Runs single-question regeneration on the {@code quizExecutor}. Assembles the full
 * context (material, characteristics, constraints, the current question, the other
 * questions), calls the generator, persists the result as a new {@link QuestionVersion}
 * and makes it the active version, recording the outcome on the question's
 * {@code regenerationStatus} so the UI can poll for progress.
 */
@Component
public class QuestionRegenerationExecutor {

    private static final Logger log = LoggerFactory.getLogger(QuestionRegenerationExecutor.class);

    private final ChapterRepository chapterRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository versionRepository;
    private final LearningObjectiveRepository learningObjectiveRepository;
    private final StructuralCharacteristicRepository structuralRepository;
    private final SurfaceCharacteristicRepository surfaceRepository;
    private final ConstraintRepository constraintRepository;
    private final FileStorageService fileStorageService;
    private final QuizGenerationService quizGenerationService;
    private final ObjectMapper objectMapper;

    public QuestionRegenerationExecutor(ChapterRepository chapterRepository,
                                        QuestionRepository questionRepository,
                                        QuestionVersionRepository versionRepository,
                                        LearningObjectiveRepository learningObjectiveRepository,
                                        StructuralCharacteristicRepository structuralRepository,
                                        SurfaceCharacteristicRepository surfaceRepository,
                                        ConstraintRepository constraintRepository,
                                        FileStorageService fileStorageService,
                                        QuizGenerationService quizGenerationService,
                                        ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.questionRepository = questionRepository;
        this.versionRepository = versionRepository;
        this.learningObjectiveRepository = learningObjectiveRepository;
        this.structuralRepository = structuralRepository;
        this.surfaceRepository = surfaceRepository;
        this.constraintRepository = constraintRepository;
        this.fileStorageService = fileStorageService;
        this.quizGenerationService = quizGenerationService;
        this.objectMapper = objectMapper;
    }

    @Async("quizExecutor")
    public void runRegeneration(int bookId, int ordinal, int questionId, String guideline) {
        Chapter chapter = chapterRepository.findByBookIdAndOrdinal(bookId, ordinal).orElse(null);
        if (chapter == null) {
            log.warn("Book {} chapter {}: vanished before question {} regeneration", bookId, ordinal, questionId);
            return;
        }
        int chapterId = chapter.getId();
        Question question = questionRepository.findByIdAndChapterId(questionId, chapterId).orElse(null);
        if (question == null) {
            log.warn("Book {} chapter {}: question {} vanished before regeneration", bookId, ordinal, questionId);
            return;
        }

        try {
            String material = new String(
                    fileStorageService.downloadFile(chapter.getMarkdownObjectKey()), StandardCharsets.UTF_8);
            List<LearningObjectiveInput> learningObjectives = loadLearningObjectives(chapterId);
            List<String> surface = surfaceRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> c.getContent()).toList();
            List<String> constraints = constraintRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> c.getContent()).toList();
            List<String> otherQuestions = questionRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .filter(q -> !q.getId().equals(questionId))
                    .map(Question::getContent)
                    .toList();

            log.info("Book {} chapter {}: regenerating question {} (guideline present: {})",
                    bookId, ordinal, questionId, guideline != null && !guideline.isBlank());

            QuestionRegenerationRequest request = new QuestionRegenerationRequest(
                    List.of(material), learningObjectives, surface, constraints,
                    question.getContent(), otherQuestions, guideline);

            RegeneratedQuestion result = quizGenerationService.regenerate(request);

            saveNewActiveVersion(questionId, result, guideline);
            log.info("Book {} chapter {}: question {} regenerated", bookId, ordinal, questionId);
        } catch (Exception e) {
            log.error("Book {} chapter {}: question {} regeneration failed", bookId, ordinal, questionId, e);
            questionRepository.findById(questionId).ifPresent(q -> {
                q.setRegenerationStatus(ProcessingStatus.FAILED);
                q.setRegenerationError(truncate(e.getMessage()));
                questionRepository.save(q);
            });
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

    private void saveNewActiveVersion(int questionId, RegeneratedQuestion result, String guideline) {
        Question question = questionRepository.findById(questionId).orElseThrow();

        QuestionVersion version = new QuestionVersion();
        version.setQuestion(question);
        version.setQuestionType(result.question().questionType());
        version.setContent(serialize(result.question()));
        version.setOrigin(QuestionOrigin.GENERATED);
        version.setStatus(QuestionStatus.PENDING);
        version.setGuideline(guideline == null || guideline.isBlank() ? null : guideline.strip());
        version = versionRepository.save(version);

        question.mirror(version);
        question.setRegenerationStatus(ProcessingStatus.DONE);
        question.setRegenerationError(null);
        questionRepository.save(question);
    }

    private String serialize(GeneratedQuestion payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize regenerated question", e);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
