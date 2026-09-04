package com.example.book2quiz.service;

import com.example.book2quiz.dto.question.QuestionGenerationStatusResponse;
import com.example.book2quiz.dto.question.QuestionResponse;
import com.example.book2quiz.dto.question.QuestionVersionDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.model.Question;
import com.example.book2quiz.model.QuestionOrigin;
import com.example.book2quiz.model.QuestionStatus;
import com.example.book2quiz.model.QuestionVersion;
import com.example.book2quiz.dto.quiz.GeneratedQuestion;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.QuestionRepository;
import com.example.book2quiz.repository.QuestionVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final ChapterRepository chapterRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository versionRepository;
    private final QuestionGenerationExecutor generationExecutor;
    private final QuestionRegenerationExecutor regenerationExecutor;
    private final QuestionVersionManager versionManager;
    private final QuizValidator quizValidator;
    private final ObjectMapper objectMapper;

    public QuestionService(ChapterRepository chapterRepository,
                           QuestionRepository questionRepository,
                           QuestionVersionRepository versionRepository,
                           QuestionGenerationExecutor generationExecutor,
                           QuestionRegenerationExecutor regenerationExecutor,
                           QuestionVersionManager versionManager,
                           QuizValidator quizValidator,
                           ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.questionRepository = questionRepository;
        this.versionRepository = versionRepository;
        this.generationExecutor = generationExecutor;
        this.regenerationExecutor = regenerationExecutor;
        this.versionManager = versionManager;
        this.quizValidator = quizValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(Integer bookId, int ordinal) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return questionRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public QuestionResponse create(Integer bookId, int ordinal, GeneratedQuestion payload) {
        validateManual(payload);
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        Question question = new Question();
        question.setChapter(chapter);
        question.setQuestionType(payload.questionType());
        question.setContent(serialize(payload));
        question.setOrigin(QuestionOrigin.MANUAL);
        question.setStatus(QuestionStatus.APPROVED);
        question = questionRepository.save(question);
        versionManager.createInitialVersion(question);
        return toResponse(question);
    }

    /** Edits the question's active version in place (and syncs the question mirror). */
    @Transactional
    public QuestionResponse update(Integer bookId, int ordinal, Integer id, GeneratedQuestion payload) {
        validateManual(payload);
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        QuestionVersion active = activeVersionOrThrow(question);
        active.setQuestionType(payload.questionType());
        active.setContent(serialize(payload));
        question.mirror(active);
        return toResponse(question);
    }

    /** Approves/reverts the question's active version (and syncs the question mirror). */
    @Transactional
    public QuestionResponse updateStatus(Integer bookId, int ordinal, Integer id, QuestionStatus status) {
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        QuestionVersion active = activeVersionOrThrow(question);
        active.setStatus(status);
        question.setStatus(status);
        return toResponse(question);
    }

    @Transactional
    public void delete(Integer bookId, int ordinal, Integer id) {
        questionRepository.delete(findQuestionOrThrow(bookId, ordinal, id));
    }

    @Transactional
    public void requestGeneration(Integer bookId, int ordinal) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        if (chapter.getMarkdownObjectKey() == null) {
            throw new IllegalArgumentException(
                    "Chapter " + ordinal + " has no Markdown yet; convert it before generating questions");
        }
        if (chapter.getQuestionGenerationStatus() == ProcessingStatus.PROCESSING) {
            throw new IllegalArgumentException("Question generation is already in progress for this chapter");
        }

        chapter.setQuestionGenerationStatus(ProcessingStatus.PROCESSING);
        chapter.setQuestionGenerationError(null);

        int bId = bookId;
        int ord = ordinal;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                generationExecutor.runGeneration(bId, ord);
            }
        });
    }

    @Transactional(readOnly = true)
    public QuestionGenerationStatusResponse getGenerationStatus(Integer bookId, int ordinal) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        return new QuestionGenerationStatusResponse(
                chapter.getQuestionGenerationStatus(), chapter.getQuestionGenerationError());
    }

    // ---- regeneration ----

    /** Validates and launches async regeneration of one question with a reviewer guideline. */
    @Transactional
    public void requestRegeneration(Integer bookId, int ordinal, Integer id, String guideline) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        if (chapter.getMarkdownObjectKey() == null) {
            throw new IllegalArgumentException(
                    "Chapter " + ordinal + " has no Markdown yet; convert it before regenerating questions");
        }
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        if (question.getRegenerationStatus() == ProcessingStatus.PROCESSING) {
            throw new IllegalArgumentException("Regeneration is already in progress for this question");
        }

        question.setRegenerationStatus(ProcessingStatus.PROCESSING);
        question.setRegenerationError(null);

        int bId = bookId;
        int ord = ordinal;
        int qId = id;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                regenerationExecutor.runRegeneration(bId, ord, qId, guideline);
            }
        });
    }

    // ---- versions ----

    @Transactional(readOnly = true)
    public List<QuestionVersionDTO> listVersions(Integer bookId, int ordinal, Integer id) {
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        Integer activeId = question.getActiveVersionId();
        return versionRepository.findByQuestionIdOrderByIdAsc(id).stream()
                .map(v -> toVersionDTO(v, activeId))
                .toList();
    }

    @Transactional
    public QuestionResponse activateVersion(Integer bookId, int ordinal, Integer id, Integer versionId) {
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        QuestionVersion version = findVersionOrThrow(id, versionId);
        question.mirror(version);
        return toResponse(question);
    }

    @Transactional
    public QuestionVersionDTO updateVersion(Integer bookId, int ordinal, Integer id,
                                            Integer versionId, GeneratedQuestion payload) {
        validateManual(payload);
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        QuestionVersion version = findVersionOrThrow(id, versionId);
        version.setQuestionType(payload.questionType());
        version.setContent(serialize(payload));
        if (versionId.equals(question.getActiveVersionId())) {
            question.mirror(version);
        }
        return toVersionDTO(version, question.getActiveVersionId());
    }

    @Transactional
    public void deleteVersion(Integer bookId, int ordinal, Integer id, Integer versionId) {
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        QuestionVersion version = findVersionOrThrow(id, versionId);
        if (versionId.equals(question.getActiveVersionId())) {
            throw new IllegalArgumentException("Cannot delete the active version; activate another version first");
        }
        if (versionRepository.countByQuestionId(id) <= 1) {
            throw new IllegalArgumentException("Cannot delete the only version of a question");
        }
        versionRepository.delete(version);
    }

    // ---- helpers ----

    private void validateManual(GeneratedQuestion payload) {
        try {
            quizValidator.validate(payload);
        } catch (com.example.book2quiz.exception.InvalidQuizOutputException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private Chapter findChapterOrThrow(Integer bookId, int ordinal) {
        return chapterRepository.findByBookIdAndOrdinal(bookId, ordinal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + ordinal + " not found for book " + bookId));
    }

    private Question findQuestionOrThrow(Integer bookId, int ordinal, Integer id) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return questionRepository.findByIdAndChapterId(id, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question " + id + " not found for chapter " + ordinal + " of book " + bookId));
    }

    private QuestionVersion findVersionOrThrow(Integer questionId, Integer versionId) {
        return versionRepository.findByIdAndQuestionId(versionId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionId + " not found for question " + questionId));
    }

    private QuestionVersion activeVersionOrThrow(Question question) {
        return findVersionOrThrow(question.getId(), question.getActiveVersionId());
    }

    private String serialize(GeneratedQuestion payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize question", e);
        }
    }

    private GeneratedQuestion deserialize(String content, String context) {
        try {
            return objectMapper.readValue(content, GeneratedQuestion.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize " + context, e);
        }
    }

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getStatus(),
                question.getOrigin(),
                deserialize(question.getContent(), "stored question " + question.getId()),
                question.getActiveVersionId(),
                (int) versionRepository.countByQuestionId(question.getId()),
                question.getRegenerationStatus(),
                question.getRegenerationError(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    private QuestionVersionDTO toVersionDTO(QuestionVersion version, Integer activeVersionId) {
        return new QuestionVersionDTO(
                version.getId(),
                version.getId().equals(activeVersionId),
                version.getStatus(),
                version.getOrigin(),
                version.getGuideline(),
                deserialize(version.getContent(), "stored version " + version.getId()),
                version.getCreatedAt(),
                version.getUpdatedAt());
    }
}
