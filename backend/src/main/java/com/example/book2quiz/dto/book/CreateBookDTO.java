package com.example.book2quiz.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record CreateBookDTO(
        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Course id is required")
        Integer courseId,

        @NotNull(message = "File is required")
        MultipartFile file
) {
}
