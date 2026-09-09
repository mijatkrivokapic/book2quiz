package com.example.book2quiz.dto.generation;

import com.example.book2quiz.model.GenerationKind;

import java.util.Map;

/**
 * Aggregated token usage for a chapter: overall totals plus per-type counts and tokens.
 */
public record GenerationUsageSummaryDTO(
        int recordCount,
        long totalInputTokens,
        long totalOutputTokens,
        long totalTokens,
        Map<GenerationKind, TypeUsage> byType
) {
    /** Per-generation-type rollup. */
    public record TypeUsage(
            int count,
            long inputTokens,
            long outputTokens,
            long totalTokens
    ) {
    }
}
