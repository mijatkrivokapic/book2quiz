package com.example.book2quiz.dto.characteristic;

import com.example.book2quiz.dto.quiz.TokenUsage;

import java.util.List;

/**
 * Domain result of a characteristic generation: the learning objectives (each with its
 * structural characteristics) and the shared surface characteristics. {@code analysis} and
 * {@code usage} are for logging/diagnostics.
 */
public record GeneratedCharacteristics(
        String analysis,
        List<GeneratedLearningObjective> learningObjectives,
        List<String> surfaceCharacteristics,
        TokenUsage usage
) {
}
