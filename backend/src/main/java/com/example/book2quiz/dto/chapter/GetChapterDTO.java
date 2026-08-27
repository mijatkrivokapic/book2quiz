package com.example.book2quiz.dto.chapter;

import com.example.book2quiz.model.ProcessingStatus;

import java.time.Instant;

public record GetChapterDTO(
        Integer id,
        int ordinal,
        String title,
        int startPage,
        int endPage,
        ProcessingStatus status,
        String errorMessage,
        boolean markdownAvailable,
        Instant createdAt,
        Instant updatedAt
) {
}
