package com.example.book2quiz.service;

import com.example.book2quiz.dto.characteristic.CharacteristicGenerationRequest;
import com.example.book2quiz.dto.characteristic.GeneratedCharacteristics;

/**
 * Port: generates structural and surface characteristics from learning material.
 * Implementations must be free of any model-provider SDK dependency at the interface level.
 */
public interface CharacteristicGenerator {

    GeneratedCharacteristics generate(CharacteristicGenerationRequest request);
}
