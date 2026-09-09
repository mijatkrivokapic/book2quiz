package com.example.book2quiz.dto.quiz;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Inputs for one quiz generation. {@code instructionalItems} is the learning material
 * (required, non-empty); the remaining lists may be null/empty.
 */
public record QuizGenerationRequest(
        @NotEmpty(message = "instructionalItems must not be empty")
        List<String> instructionalItems,

        List<String> localConstraints,

        List<LearningObjectiveInput> learningObjectives,

        List<String> surfaceCharacteristics
) {
    /** Null-safe accessors so callers and the prompt builder never see nulls. */
    public List<String> instructionalItemsOrEmpty() {
        return instructionalItems == null ? List.of() : instructionalItems;
    }

    public List<String> localConstraintsOrEmpty() {
        return localConstraints == null ? List.of() : localConstraints;
    }

    public List<LearningObjectiveInput> learningObjectivesOrEmpty() {
        return learningObjectives == null ? List.of() : learningObjectives;
    }

    public List<String> surfaceCharacteristicsOrEmpty() {
        return surfaceCharacteristics == null ? List.of() : surfaceCharacteristics;
    }
}
