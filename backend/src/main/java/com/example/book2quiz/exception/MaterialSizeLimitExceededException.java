package com.example.book2quiz.exception;

/** The combined instructional material exceeded the configured character limit. */
public class MaterialSizeLimitExceededException extends QuizGenerationException {

    public MaterialSizeLimitExceededException(String message) {
        super(message);
    }
}
