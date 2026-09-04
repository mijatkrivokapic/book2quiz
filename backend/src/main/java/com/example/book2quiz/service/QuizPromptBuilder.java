package com.example.book2quiz.service;

import com.example.book2quiz.dto.quiz.QuestionRegenerationRequest;
import com.example.book2quiz.dto.quiz.QuizGenerationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the user message from the request by joining XML sections in a fixed order:
 * instructional_items, structural_characteristics, surface_characteristics, constraints
 * (global constraints from the database first, then per-request local constraints).
 * Empty sections are omitted entirely.
 */
@Component
public class QuizPromptBuilder {

    private final GlobalConstraintService globalConstraintService;

    public QuizPromptBuilder(GlobalConstraintService globalConstraintService) {
        this.globalConstraintService = globalConstraintService;
    }

    public String buildUserMessage(QuizGenerationRequest request) {
        List<String> constraints = new ArrayList<>(globalConstraintService.getContents());
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

    /**
     * Builds the user message for regenerating a single question: the full generation
     * context plus the question being revised, the other questions (avoid overlap), and
     * the reviewer's guideline.
     */
    public String buildRegenerationUserMessage(QuestionRegenerationRequest request) {
        List<String> constraints = new ArrayList<>(globalConstraintService.getContents());
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
        addIfPresent(sections, XmlSection.of(
                "question_to_revise", "question", List.of(nullToEmpty(request.currentQuestionJson()))));
        addIfPresent(sections, XmlSection.of(
                "other_questions", "question", request.otherQuestionsJsonOrEmpty()));
        addIfPresent(sections, XmlSection.of(
                "revision_guidelines", "revision_guideline", List.of(nullToEmpty(request.guideline()))));

        return String.join("\n\n", sections);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void addIfPresent(List<String> sections, String section) {
        if (!section.isEmpty()) {
            sections.add(section);
        }
    }
}
