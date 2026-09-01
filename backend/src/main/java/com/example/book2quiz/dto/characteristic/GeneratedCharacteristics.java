package com.example.book2quiz.dto.characteristic;

import com.example.book2quiz.dto.quiz.TokenUsage;

import java.util.List;

/**
 * Domain result of a characteristic generation. {@code analysis} and {@code usage} are
 * for logging/diagnostics.
 */
public record GeneratedCharacteristics(
        String analysis,
        List<String> structuralCharacteristics,
        List<String> surfaceCharacteristics,
        TokenUsage usage
) {
}
