package com.example.book2quiz.service;

import com.example.book2quiz.dto.quiz.LearningObjectiveInput;
import com.example.book2quiz.dto.quiz.QuestionRegenerationRequest;
import com.example.book2quiz.dto.quiz.QuizGenerationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the user message from the request by joining XML sections in a fixed order:
 * instructional_items, learning_objectives (each with its nested structural_characteristics),
 * surface_characteristics, constraints (global constraints from the database first, then
 * per-request local constraints). Empty sections are omitted entirely.
 */
@Component
public class QuizPromptBuilder {

    private final GlobalConstraintService globalConstraintService;

    public QuizPromptBuilder(GlobalConstraintService globalConstraintService) {
        this.globalConstraintService = globalConstraintService;
    }

    public PromptContent buildUserMessage(QuizGenerationRequest request) {
        List<String> constraints = new ArrayList<>(globalConstraintService.getContents());
        constraints.addAll(request.localConstraintsOrEmpty());

        String material = XmlSection.of(
                "instructional_items", "instructional_item", request.instructionalItemsOrEmpty());

        List<String> contextSections = new ArrayList<>();
        addIfPresent(contextSections, buildLearningObjectivesSection(request.learningObjectivesOrEmpty()));
        addIfPresent(contextSections, XmlSection.of(
                "surface_characteristics", "surface_characteristic", request.surfaceCharacteristicsOrEmpty()));
        addIfPresent(contextSections, XmlSection.of(
                "constraints", "constraint", constraints));

        // Generation has no volatile tail; material and context each get their own cache tier.
        return new PromptContent(material, String.join("\n\n", contextSections), "");
    }

    /**
     * A user message split into cache tiers, most-stable first, so each gets its own cache
     * breakpoint (used by both generation and regeneration):
     * <ul>
     *   <li>{@code material} — the chapter's instructional items (large, rarely edited);</li>
     *   <li>{@code context} — learning objectives, surface characteristics and constraints
     *       (edited more often than the material);</li>
     *   <li>{@code volatileSuffix} — the per-request tail, only for regeneration (the question
     *       being revised, the other questions, the reviewer's guideline); never cached. Empty
     *       for generation.</li>
     * </ul>
     * Because {@code material} precedes {@code context} in the prompt, editing the characteristics
     * or constraints invalidates only the {@code context} cache — the material cache still hits.
     */
    public record PromptContent(String material, String context, String volatileSuffix) {
        /** All parts joined into one message (used when caching is off). */
        public String joined() {
            StringBuilder sb = new StringBuilder();
            for (String part : new String[]{material, context, volatileSuffix}) {
                if (part != null && !part.isBlank()) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append(part);
                }
            }
            return sb.toString();
        }
    }

    /**
     * Builds the user message for regenerating a single question: the full generation
     * context plus the question being revised, the other questions (avoid overlap), and
     * the reviewer's guideline. Split into cache tiers — see {@link PromptContent}.
     */
    public PromptContent buildRegenerationUserMessage(QuestionRegenerationRequest request) {
        List<String> constraints = new ArrayList<>(globalConstraintService.getContents());
        constraints.addAll(request.localConstraintsOrEmpty());

        String material = XmlSection.of(
                "instructional_items", "instructional_item", request.instructionalItemsOrEmpty());

        List<String> contextSections = new ArrayList<>();
        addIfPresent(contextSections, buildLearningObjectivesSection(request.learningObjectivesOrEmpty()));
        addIfPresent(contextSections, XmlSection.of(
                "surface_characteristics", "surface_characteristic", request.surfaceCharacteristicsOrEmpty()));
        addIfPresent(contextSections, XmlSection.of(
                "constraints", "constraint", constraints));

        List<String> suffix = new ArrayList<>();
        addIfPresent(suffix, XmlSection.of(
                "question_to_revise", "question", List.of(nullToEmpty(request.currentQuestionJson()))));
        addIfPresent(suffix, XmlSection.of(
                "other_questions", "question", request.otherQuestionsJsonOrEmpty()));
        addIfPresent(suffix, XmlSection.of(
                "revision_guidelines", "revision_guideline", List.of(nullToEmpty(request.guideline()))));

        return new PromptContent(material, String.join("\n\n", contextSections), String.join("\n\n", suffix));
    }

    /**
     * Builds the {@code <learning_objectives>} section: one {@code <learning_objective>} per
     * objective, each with its {@code <description>} and a nested
     * {@code <structural_characteristics>} block. Objectives without a description are
     * skipped; an objective with no structural characteristics still renders (with an empty
     * {@code <structural_characteristics>} block). Content is sanitized so it cannot break
     * out of its tag. Returns an empty string when there are no objectives, so the caller
     * omits the section.
     */
    private String buildLearningObjectivesSection(List<LearningObjectiveInput> objectives) {
        List<LearningObjectiveInput> present = objectives.stream()
                .filter(o -> o != null && o.description() != null && !o.description().isBlank())
                .toList();
        if (present.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("<learning_objectives>\n");
        for (LearningObjectiveInput objective : present) {
            sb.append("\t<learning_objective>\n");
            sb.append("\t\t<description>\n");
            sb.append("\t\t\t").append(XmlSection.sanitize(objective.description().strip())).append('\n');
            sb.append("\t\t</description>\n");
            sb.append("\t\t<structural_characteristics>\n");
            for (String characteristic : objective.structuralCharacteristicsOrEmpty()) {
                if (characteristic == null || characteristic.isBlank()) {
                    continue;
                }
                sb.append("\t\t\t<structural_characteristic>\n");
                sb.append("\t\t\t\t").append(XmlSection.sanitize(characteristic.strip())).append('\n');
                sb.append("\t\t\t</structural_characteristic>\n");
            }
            sb.append("\t\t</structural_characteristics>\n");
            sb.append("\t</learning_objective>\n");
        }
        sb.append("</learning_objectives>");
        return sb.toString();
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
