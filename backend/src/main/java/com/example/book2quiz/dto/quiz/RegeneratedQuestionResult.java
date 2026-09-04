package com.example.book2quiz.dto.quiz;

/** Parsed model output for a single-question regeneration: analysis + one question. */
public record RegeneratedQuestionResult(
        String analysis,
        GeneratedQuestion question
) {
}
