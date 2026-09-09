package com.example.book2quiz.service;

import com.example.book2quiz.config.CharacteristicProperties;
import com.example.book2quiz.dto.characteristic.CharacteristicGenerationRequest;
import com.example.book2quiz.dto.characteristic.GeneratedCharacteristics;
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
 * Orchestrates characteristic generation: validate the request, enforce the material
 * size limit, delegate to the {@link CharacteristicGenerator} port, and log the model,
 * prompt version, duration and token usage.
 */
@Service
public class CharacteristicGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CharacteristicGenerationService.class);

    private final CharacteristicGenerator generator;
    private final CharacteristicProperties properties;
    private final Validator validator;

    public CharacteristicGenerationService(CharacteristicGenerator generator,
                                           CharacteristicProperties properties,
                                           Validator validator) {
        this.generator = generator;
        this.properties = properties;
        this.validator = validator;
    }

    public GeneratedCharacteristics generate(CharacteristicGenerationRequest request) {
        validateRequest(request);
        enforceMaterialSizeLimit(request);

        long start = System.currentTimeMillis();
        GeneratedCharacteristics result = generator.generate(request);
        long durationMs = System.currentTimeMillis() - start;

        TokenUsage usage = result.usage();
        int structuralCount = result.learningObjectives().stream()
                .mapToInt(lo -> lo.structuralCharacteristics() == null ? 0 : lo.structuralCharacteristics().size())
                .sum();
        log.info("Characteristics generated: model={} promptVersion={} durationMs={} inputTokens={} outputTokens={} objectives={} structural={} surface={}",
                usage.model(), properties.getPromptVersion(), durationMs,
                usage.inputTokens(), usage.outputTokens(),
                result.learningObjectives().size(), structuralCount, result.surfaceCharacteristics().size());
        log.debug("Characteristic planning analysis (internal): {}", result.analysis());

        return result;
    }

    private void validateRequest(CharacteristicGenerationRequest request) {
        if (request == null) {
            throw new QuizGenerationException("Characteristic generation request must not be null");
        }
        Set<ConstraintViolation<CharacteristicGenerationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new QuizGenerationException("Invalid characteristic generation request: " + message);
        }
    }

    private void enforceMaterialSizeLimit(CharacteristicGenerationRequest request) {
        int totalChars = request.instructionalItemsOrEmpty().stream()
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
