package com.example.book2quiz.quiz.dto;

import java.util.List;

public record MrqOption(
        String text,
        boolean isCorrect,
        String feedback,
        List<String> hints
) {
}
