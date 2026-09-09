package com.example.book2quiz.dto.quiz;

/**
 * Provider-agnostic token usage for a single generation. {@code cacheCreationInputTokens}
 * and {@code cacheReadInputTokens} report prompt-caching activity (written vs served from
 * cache); both are 0 when caching is off or unused.
 */
public record TokenUsage(
        String model,
        long inputTokens,
        long outputTokens,
        long cacheCreationInputTokens,
        long cacheReadInputTokens
) {
}
