package com.example.book2quiz.dto.characteristic;

import com.example.book2quiz.model.CharacteristicStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCharacteristicStatusRequest(
        @NotNull(message = "status is required")
        CharacteristicStatus status
) {
}
