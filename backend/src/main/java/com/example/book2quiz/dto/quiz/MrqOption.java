package com.example.book2quiz.dto.quiz;

import java.util.List;

public record MrqOption(
        String text,
        boolean isCorrect,
        String feedback,
        List<String> hints
) {
}
