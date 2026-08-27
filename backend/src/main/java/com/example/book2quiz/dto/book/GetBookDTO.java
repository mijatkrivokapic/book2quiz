package com.example.book2quiz.dto.book;

public record GetBookDTO(
        Integer id,
        String title,
        Integer courseId,
        String downloadUrl
) {
}
