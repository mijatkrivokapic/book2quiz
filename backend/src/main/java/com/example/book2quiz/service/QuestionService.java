package com.example.book2quiz.service;

import com.example.book2quiz.dto.question.QuestionGenerationStatusResponse;
import com.example.book2quiz.dto.question.QuestionResponse;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.model.Question;
import com.example.book2quiz.model.QuestionOrigin;
import com.example.book2quiz.model.QuestionStatus;
import com.example.book2quiz.dto.quiz.GeneratedQuestion;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.QuestionRepository;
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
    private final QuestionGenerationExecutor generationExecutor;
    private final QuizValidator quizValidator;
    private final ObjectMapper objectMapper;

    public QuestionService(ChapterRepository chapterRepository,
                           QuestionRepository questionRepository,
                           QuestionGenerationExecutor generationExecutor,
                           QuizValidator quizValidator,
                           ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.questionRepository = questionRepository;
        this.generationExecutor = generationExecutor;
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
        Question saved = persist(chapter, payload, QuestionOrigin.MANUAL, QuestionStatus.APPROVED);
        return toResponse(saved);
    }

    @Transactional
    public QuestionResponse update(Integer bookId, int ordinal, Integer id, GeneratedQuestion payload) {
        validateManual(payload);
        Question question = findQuestionOrThrow(bookId, ordinal, id);
        question.setQuestionType(payload.questionType());
        question.setContent(serialize(payload));
        return toResponse(question);
    }

    @Transactional
    public QuestionResponse updateStatus(Integer bookId, int ordinal, Integer id, QuestionStatus status) {
        Question question = findQuestionOrThrow(bookId, ordinal, id);
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

    /** Validates a manually submitted question, surfacing problems as a 400 (not a 502). */
    private void validateManual(GeneratedQuestion payload) {
        try {
            quizValidator.validate(payload);
        } catch (com.example.book2quiz.exception.InvalidQuizOutputException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private Question persist(Chapter chapter, GeneratedQuestion payload, QuestionOrigin origin, QuestionStatus status) {
        Question question = new Question();
        question.setChapter(chapter);
        question.setQuestionType(payload.questionType());
        question.setContent(serialize(payload));
        question.setOrigin(origin);
        question.setStatus(status);
        return questionRepository.save(question);
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

    private String serialize(GeneratedQuestion payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize question", e);
        }
    }

    private QuestionResponse toResponse(Question question) {
        GeneratedQuestion payload;
        try {
            payload = objectMapper.readValue(question.getContent(), GeneratedQuestion.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored question " + question.getId(), e);
        }
        return new QuestionResponse(
                question.getId(),
                question.getStatus(),
                question.getOrigin(),
                payload,
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
