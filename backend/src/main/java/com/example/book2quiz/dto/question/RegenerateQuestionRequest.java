package com.example.book2quiz.dto.question;

/**
 * Reviewer's extra guideline for regenerating a question. Optional — a blank guideline
 * means "just try again" with the same context.
 */
public record RegenerateQuestionRequest(
        String guideline
) {
}
