package com.example.book2quiz.service;

import com.example.book2quiz.dto.quiz.GeneratedQuiz;
import com.example.book2quiz.dto.quiz.QuizGenerationRequest;

/**
 * Port: generates a quiz from a request. Implementations must be free of any specific
 * model-provider SDK dependency at the interface level.
 */
public interface QuizGenerator {

    GeneratedQuiz generate(QuizGenerationRequest request);
}
