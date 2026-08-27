package com.example.book2quiz.dto.course;

import jakarta.validation.constraints.NotBlank;

public record UpdateCourseDTO(

        @NotBlank(message = "Name is required")
        String name

) {
}
