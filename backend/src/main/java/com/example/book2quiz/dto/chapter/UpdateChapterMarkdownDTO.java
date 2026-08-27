package com.example.book2quiz.dto.chapter;

import jakarta.validation.constraints.NotNull;

public record UpdateChapterMarkdownDTO(
        @NotNull(message = "Content is required")
        String content
) {
}
