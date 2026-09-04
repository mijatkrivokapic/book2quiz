package com.example.book2quiz.dto.question;

import com.example.book2quiz.dto.quiz.GeneratedQuestion;
import com.example.book2quiz.model.QuestionOrigin;
import com.example.book2quiz.model.QuestionStatus;

import java.time.Instant;

public record QuestionVersionDTO(
        Integer id,
        boolean active,
        QuestionStatus status,
        QuestionOrigin origin,
        String guideline,
        GeneratedQuestion question,
        Instant createdAt,
        Instant updatedAt
) {
}
