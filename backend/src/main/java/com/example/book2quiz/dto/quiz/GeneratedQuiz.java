package com.example.book2quiz.dto.quiz;


import java.util.List;

/**
 * Domain result of a quiz generation. {@code analysis} and {@code usage} are for
 * logging/diagnostics; a user-facing layer should expose only {@code questions}.
 */
public record GeneratedQuiz(
        String analysis,
        List<GeneratedQuestion> questions,
        TokenUsage usage
) {
}
