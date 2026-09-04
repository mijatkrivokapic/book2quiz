package com.example.book2quiz.dto.quiz;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Inputs for regenerating a single question. Carries the full context (material,
 * characteristics, constraints), the current question being revised, the other existing
 * questions (to avoid overlap), and the reviewer's extra guideline.
 */
public record QuestionRegenerationRequest(
        @NotEmpty(message = "instructionalItems must not be empty")
        List<String> instructionalItems,

        List<String> structuralCharacteristics,

        List<String> surfaceCharacteristics,

        List<String> localConstraints,

        /** JSON of the question being revised. */
        String currentQuestionJson,

        /** JSON of each of the other questions in the chapter (avoid overlap). */
        List<String> otherQuestionsJson,

        /** Free-text reviewer guideline; may be null/blank (plain retry). */
        String guideline
) {
    public List<String> instructionalItemsOrEmpty() {
        return instructionalItems == null ? List.of() : instructionalItems;
    }

    public List<String> structuralCharacteristicsOrEmpty() {
        return structuralCharacteristics == null ? List.of() : structuralCharacteristics;
    }

    public List<String> surfaceCharacteristicsOrEmpty() {
        return surfaceCharacteristics == null ? List.of() : surfaceCharacteristics;
    }

    public List<String> localConstraintsOrEmpty() {
        return localConstraints == null ? List.of() : localConstraints;
    }

    public List<String> otherQuestionsJsonOrEmpty() {
        return otherQuestionsJson == null ? List.of() : otherQuestionsJson;
    }
}
