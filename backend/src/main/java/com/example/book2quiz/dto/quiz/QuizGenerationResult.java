package com.example.book2quiz.dto.quiz;

import java.util.List;

/**
 * Root DTO the model's JSON is parsed into. {@code analysis} is the model's planning
 * step — logged, never returned to the end user.
 */
public record QuizGenerationResult(
        String analysis,
        List<GeneratedQuestion> questions
) {
}
