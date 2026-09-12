package com.example.book2quiz.dto.generation;

import com.example.book2quiz.model.ProcessingStatus;

/**
 * A server-sent event describing a change in an async job for a book: chapter extraction,
 * or characteristic / question / single-question-regeneration generation. Pushed to the
 * frontend over SSE so it can update without polling.
 */
public record GenerationEvent(
        Kind kind,
        Integer ordinal,     // chapter ordinal; null for book-level EXTRACTION
        Integer questionId,  // only for REGENERATION
        ProcessingStatus status,
        String error
) {
    public enum Kind { CHAPTER, EXTRACTION, CHARACTERISTICS, QUESTIONS, REGENERATION }

    public static GenerationEvent chapter(int ordinal, ProcessingStatus status, String error) {
        return new GenerationEvent(Kind.CHAPTER, ordinal, null, status, error);
    }

    public static GenerationEvent extraction(ProcessingStatus status) {
        return new GenerationEvent(Kind.EXTRACTION, null, null, status, null);
    }

    public static GenerationEvent characteristics(int ordinal, ProcessingStatus status, String error) {
        return new GenerationEvent(Kind.CHARACTERISTICS, ordinal, null, status, error);
    }

    public static GenerationEvent questions(int ordinal, ProcessingStatus status, String error) {
        return new GenerationEvent(Kind.QUESTIONS, ordinal, null, status, error);
    }

    public static GenerationEvent regeneration(int ordinal, int questionId, ProcessingStatus status, String error) {
        return new GenerationEvent(Kind.REGENERATION, ordinal, questionId, status, error);
    }
}
