package com.example.book2quiz.dto.quiz;

/** Provider-agnostic token usage for a single generation, for logging. */
public record TokenUsage(
        String model,
        long inputTokens,
        long outputTokens
) {
}
