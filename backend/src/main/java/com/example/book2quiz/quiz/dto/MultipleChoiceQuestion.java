package com.example.book2quiz.quiz.dto;

import java.util.List;

public record MultipleChoiceQuestion(
        String text,
        List<String> distractors,
        String correctOption,
        String feedback,
        List<String> hints
) implements GeneratedQuestion {
}
