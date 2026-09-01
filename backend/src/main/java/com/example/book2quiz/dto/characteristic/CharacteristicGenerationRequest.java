package com.example.book2quiz.dto.characteristic;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Inputs for one characteristic generation. Only the learning material is provided —
 * this prompt appends nothing but {@code instructional_items}.
 */
public record CharacteristicGenerationRequest(
        @NotEmpty(message = "instructionalItems must not be empty")
        List<String> instructionalItems
) {
    public List<String> instructionalItemsOrEmpty() {
        return instructionalItems == null ? List.of() : instructionalItems;
    }
}
