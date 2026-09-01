package com.example.book2quiz.dto.question;

import com.example.book2quiz.model.QuestionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionStatusRequest(
        @NotNull(message = "status is required")
        QuestionStatus status
) {
}
