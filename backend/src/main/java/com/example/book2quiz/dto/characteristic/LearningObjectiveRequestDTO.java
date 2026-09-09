package com.example.book2quiz.dto.characteristic;

import jakarta.validation.constraints.NotBlank;

public record LearningObjectiveRequestDTO(
        @NotBlank(message = "Description is required")
        String description
) {
}
