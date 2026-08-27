package com.example.book2quiz.dto.constraint;

import jakarta.validation.constraints.NotBlank;

public record ConstraintRequestDTO(
        @NotBlank(message = "Content is required")
        String content
) {
}
