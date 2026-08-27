package com.example.book2quiz.dto.book;

import jakarta.validation.constraints.NotBlank;

public record UpdateBookDTO(

        @NotBlank(message = "Title is required")
        String title

) {
}
