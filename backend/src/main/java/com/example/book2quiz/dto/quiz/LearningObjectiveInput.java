package com.example.book2quiz.dto.quiz;

import java.util.List;

/**
 * One learning objective as sent into the quiz-generation prompt: its description and the
 * structural characteristics that belong to it.
 */
public record LearningObjectiveInput(
        String description,
        List<String> structuralCharacteristics
) {
    public List<String> structuralCharacteristicsOrEmpty() {
        return structuralCharacteristics == null ? List.of() : structuralCharacteristics;
    }
}
