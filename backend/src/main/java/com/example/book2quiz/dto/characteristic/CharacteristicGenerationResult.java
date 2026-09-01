package com.example.book2quiz.dto.characteristic;

import java.util.List;

/** Parsed model output: an analysis plus the two characteristic lists. */
public record CharacteristicGenerationResult(
        String analysis,
        List<String> structuralCharacteristics,
        List<String> surfaceCharacteristics
) {
}
