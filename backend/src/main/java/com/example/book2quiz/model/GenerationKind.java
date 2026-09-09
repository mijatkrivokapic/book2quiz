package com.example.book2quiz.model;

/**
 * What an LLM generation produced, for the per-chapter generation/usage history.
 *
 * <p>Named {@code GenerationKind} (not {@code GenerationType}) on purpose: a same-package
 * {@code GenerationType} would shadow {@code jakarta.persistence.GenerationType} for every
 * entity's {@code @GeneratedValue(strategy = GenerationType.IDENTITY)}.
 */
public enum GenerationKind {
    CHARACTERISTICS,
    QUESTIONS,
    QUESTION_REGENERATION
}
