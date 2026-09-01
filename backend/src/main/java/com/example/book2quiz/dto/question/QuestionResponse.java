package com.example.book2quiz.dto.question;

import com.example.book2quiz.model.QuestionOrigin;
import com.example.book2quiz.model.QuestionStatus;
import com.example.book2quiz.dto.quiz.GeneratedQuestion;

import java.time.Instant;

public record QuestionResponse(
        Integer id,
        QuestionStatus status,
        QuestionOrigin origin,
        GeneratedQuestion question,
        Instant createdAt,
        Instant updatedAt
) {
}
