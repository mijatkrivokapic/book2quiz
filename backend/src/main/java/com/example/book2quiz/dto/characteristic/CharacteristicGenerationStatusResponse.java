package com.example.book2quiz.dto.characteristic;

import com.example.book2quiz.model.ProcessingStatus;

/**
 * Progress of the async characteristic-generation job. {@code status} is null when
 * generation has never been requested.
 */
public record CharacteristicGenerationStatusResponse(
        ProcessingStatus status,
        String error
) {
}
