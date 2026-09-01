package com.example.book2quiz.service;

import com.example.book2quiz.dto.quiz.QuizGenerationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the user message from the request by joining XML sections in a fixed order:
 * instructional_items, structural_characteristics, surface_characteristics, constraints
 * (global base constraints first, then per-request local constraints). Empty sections
 * are omitted entirely.
 */
@Component
public class QuizPromptBuilder {

    private final QuizPromptLoader promptLoader;

    public QuizPromptBuilder(QuizPromptLoader promptLoader) {
        this.promptLoader = promptLoader;
    }

    public String buildUserMessage(QuizGenerationRequest request) {
        List<String> constraints = new ArrayList<>(promptLoader.getBaseConstraints());
        constraints.addAll(request.localConstraintsOrEmpty());

        List<String> sections = new ArrayList<>();
        addIfPresent(sections, XmlSection.of(
                "instructional_items", "instructional_item", request.instructionalItemsOrEmpty()));
        addIfPresent(sections, XmlSection.of(
                "structural_characteristics", "structural_characteristic", request.structuralCharacteristicsOrEmpty()));
        addIfPresent(sections, XmlSection.of(
                "surface_characteristics", "surface_characteristic", request.surfaceCharacteristicsOrEmpty()));
        addIfPresent(sections, XmlSection.of(
                "constraints", "constraint", constraints));

        return String.join("\n\n", sections);
    }

    private void addIfPresent(List<String> sections, String section) {
        if (!section.isEmpty()) {
            sections.add(section);
        }
    }
}
