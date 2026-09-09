package com.example.book2quiz.dto.generation;

import com.example.book2quiz.model.GenerationKind;

import java.time.Instant;

public record GenerationRecordDTO(
        Integer id,
        GenerationKind type,
        String model,
        String promptVersion,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        long cacheCreationInputTokens,
        long cacheReadInputTokens,
        long durationMs,
        String summary,
        Instant createdAt
) {
}
