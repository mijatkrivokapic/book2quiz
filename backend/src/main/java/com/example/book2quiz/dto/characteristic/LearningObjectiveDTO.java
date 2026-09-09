package com.example.book2quiz.dto.characteristic;

import com.example.book2quiz.model.CharacteristicOrigin;
import com.example.book2quiz.model.CharacteristicStatus;

public record LearningObjectiveDTO(
        Integer id,
        String description,
        CharacteristicStatus status,
        CharacteristicOrigin origin
) {
}
