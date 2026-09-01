package com.example.book2quiz.dto.characteristic;

import com.example.book2quiz.model.CharacteristicOrigin;
import com.example.book2quiz.model.CharacteristicStatus;

public record CharacteristicDTO(
        Integer id,
        String content,
        CharacteristicStatus status,
        CharacteristicOrigin origin
) {
}
