package com.example.book2quiz.exception;

/** The model's output could not be parsed, or failed result validation. */
public class InvalidQuizOutputException extends QuizGenerationException {

    public InvalidQuizOutputException(String message) {
        super(message);
    }

    public InvalidQuizOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
