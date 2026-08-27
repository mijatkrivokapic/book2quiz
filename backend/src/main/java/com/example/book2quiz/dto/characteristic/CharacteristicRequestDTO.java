package com.example.book2quiz.dto.characteristic;

import jakarta.validation.constraints.NotBlank;

public record CharacteristicRequestDTO(
        @NotBlank(message = "Content is required")
        String content
) {
}
