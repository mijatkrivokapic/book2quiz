package com.example.book2quiz.service;

import com.example.book2quiz.config.QuizProperties;
import com.example.book2quiz.dto.quiz.GeneratedQuiz;
import com.example.book2quiz.dto.quiz.QuestionRegenerationRequest;
import com.example.book2quiz.dto.quiz.QuizGenerationRequest;
import com.example.book2quiz.dto.quiz.RegeneratedQuestion;
import com.example.book2quiz.dto.quiz.TokenUsage;
import com.example.book2quiz.exception.MaterialSizeLimitExceededException;
import com.example.book2quiz.exception.QuizGenerationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates quiz generation: validate the request, enforce the material size limit,
 * delegate to the {@link QuizGenerator} port, validate the parsed result, and log the
 * model, prompt version, duration and token usage. The {@code analysis} is logged but
 * never returned to the end user beyond this domain result.
 */
@Service
public class QuizGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuizGenerationService.class);

    private final QuizGenerator quizGenerator;
    private final QuizValidator quizValidator;
    private final QuizProperties properties;
    private final Validator validator;

    public QuizGenerationService(QuizGenerator quizGenerator,
                                 QuizValidator quizValidator,
                                 QuizProperties properties,
                                 Validator validator) {
        this.quizGenerator = quizGenerator;
        this.quizValidator = quizValidator;
        this.properties = properties;
        this.validator = validator;
    }

    public GeneratedQuiz generate(QuizGenerationRequest request) {
        validateRequest(request);
        enforceMaterialSizeLimit(request.instructionalItemsOrEmpty());

        long start = System.currentTimeMillis();
        GeneratedQuiz quiz = quizGenerator.generate(request);
        long durationMs = System.currentTimeMillis() - start;

        TokenUsage usage = quiz.usage();
        log.info("Quiz generated: model={} promptVersion={} durationMs={} inputTokens={} outputTokens={} questions={}",
                usage.model(), properties.getPromptVersion(), durationMs,
                usage.inputTokens(), usage.outputTokens(), quiz.questions().size());
        log.debug("Quiz planning analysis (internal, not returned to user): {}", quiz.analysis());

        quizValidator.validate(quiz.questions());
        return quiz;
    }

    /** Regenerates a single question: validate context, enforce size limit, call the port,
     * validate the one question, log usage. */
    public RegeneratedQuestion regenerate(QuestionRegenerationRequest request) {
        if (request == null) {
            throw new QuizGenerationException("Regeneration request must not be null");
        }
        Set<ConstraintViolation<QuestionRegenerationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new QuizGenerationException("Invalid regeneration request: " + message);
        }
        enforceMaterialSizeLimit(request.instructionalItemsOrEmpty());

        long start = System.currentTimeMillis();
        RegeneratedQuestion result = quizGenerator.regenerate(request);
        long durationMs = System.currentTimeMillis() - start;

        TokenUsage usage = result.usage();
        log.info("Question regenerated: model={} promptVersion={} durationMs={} inputTokens={} outputTokens={}",
                usage.model(), properties.getPromptVersion(), durationMs, usage.inputTokens(), usage.outputTokens());
        log.debug("Regeneration analysis (internal, not returned to user): {}", result.analysis());

        quizValidator.validate(result.question());
        return result;
    }

    private void validateRequest(QuizGenerationRequest request) {
        if (request == null) {
            throw new QuizGenerationException("Quiz generation request must not be null");
        }
        Set<ConstraintViolation<QuizGenerationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new QuizGenerationException("Invalid quiz generation request: " + message);
        }
    }

    private void enforceMaterialSizeLimit(java.util.List<String> instructionalItems) {
        int totalChars = instructionalItems.stream()
                .filter(item -> item != null)
                .mapToInt(String::length)
                .sum();
        int limit = properties.getMaxMaterialChars();
        if (totalChars > limit) {
            throw new MaterialSizeLimitExceededException(
                    "Instructional material is too large: " + totalChars + " characters exceeds the limit of " + limit);
        }
    }
}
