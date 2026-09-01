package com.example.book2quiz.dto.question;

import com.example.book2quiz.model.ProcessingStatus;

/**
 * Progress of the async question-generation job for a chapter. {@code status} is null
 * when generation has never been requested.
 */
public record QuestionGenerationStatusResponse(
        ProcessingStatus status,
        String error
) {
}
