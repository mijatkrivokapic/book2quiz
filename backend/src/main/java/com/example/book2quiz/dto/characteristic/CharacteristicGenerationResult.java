package com.example.book2quiz.dto.characteristic;

import java.util.List;

/** Parsed model output: an analysis, the learning objectives (each with its structural characteristics), and the shared surface characteristics. */
public record CharacteristicGenerationResult(
        String analysis,
        List<GeneratedLearningObjective> learningObjectives,
        List<String> surfaceCharacteristics
) {
}
