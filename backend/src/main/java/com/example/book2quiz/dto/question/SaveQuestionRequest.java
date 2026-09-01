package com.example.book2quiz.dto.question;

import com.example.book2quiz.dto.quiz.GeneratedQuestion;
import jakarta.validation.constraints.NotNull;

public record SaveQuestionRequest(
        @NotNull(message = "question is required")
        GeneratedQuestion question
) {
}
