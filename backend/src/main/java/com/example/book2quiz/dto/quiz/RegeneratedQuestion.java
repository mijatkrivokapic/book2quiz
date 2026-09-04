package com.example.book2quiz.dto.quiz;

/**
 * Domain result of a single-question regeneration. {@code analysis} and {@code usage}
 * are for logging/diagnostics; only {@code question} is user-facing.
 */
public record RegeneratedQuestion(
        String analysis,
        GeneratedQuestion question,
        TokenUsage usage
) {
}
