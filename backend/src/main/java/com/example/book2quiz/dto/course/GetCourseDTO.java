package com.example.book2quiz.dto.course;


public record GetCourseDTO(
        Integer id,
        String name,
        int bookCount,
        int lessonCount
) {
}
