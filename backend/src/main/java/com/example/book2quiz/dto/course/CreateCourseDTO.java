package com.example.book2quiz.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseDTO(
        @NotBlank(message = "Name is required")
        String name
) {
}
