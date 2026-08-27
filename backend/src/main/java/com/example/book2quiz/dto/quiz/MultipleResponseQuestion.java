package com.example.book2quiz.dto.quiz;

import java.util.List;

public record MultipleResponseQuestion(
        String text,
        List<MrqOption> options,
        List<String> hints
) implements GeneratedQuestion {
}
