package com.example.book2quiz.dto.quiz;

import java.util.List;

public record ShortAnswerQuestion(
        String text,
        List<String> acceptableAnswers,
        String feedback,
        List<String> hints
) implements GeneratedQuestion {
}
