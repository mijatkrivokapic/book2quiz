package com.example.book2quiz.exception;

/** The call to the model provider failed (auth, rate limit, network, server error). */
public class QuizApiException extends QuizGenerationException {

    public QuizApiException(String message) {
        super(message);
    }

    public QuizApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
