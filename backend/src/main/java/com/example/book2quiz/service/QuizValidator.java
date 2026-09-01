package com.example.book2quiz.service;

import com.example.book2quiz.dto.quiz.GeneratedQuestion;
import com.example.book2quiz.dto.quiz.MrqOption;
import com.example.book2quiz.dto.quiz.MultipleChoiceQuestion;
import com.example.book2quiz.dto.quiz.MultipleResponseQuestion;
import com.example.book2quiz.dto.quiz.ShortAnswerQuestion;
import com.example.book2quiz.exception.InvalidQuizOutputException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class QuizValidator {

    public void validate(List<GeneratedQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new InvalidQuizOutputException("Generated quiz has no questions");
        }
        for (int i = 0; i < questions.size(); i++) {
            validateAt(questions.get(i), i + 1);
        }
    }


    public void validate(GeneratedQuestion question) {
        if (question == null) {
            throw new InvalidQuizOutputException("Question must not be null");
        }
        validateAt(question, 1);
    }

    private void validateAt(GeneratedQuestion question, int number) {
        if (question.text() == null || question.text().isBlank()) {
            throw new InvalidQuizOutputException("Question " + number + " has no text");
        }
        switch (question) {
            case MultipleChoiceQuestion mcq -> validateMultipleChoice(mcq, number);
            case MultipleResponseQuestion mrq -> validateMultipleResponse(mrq, number);
            case ShortAnswerQuestion saq -> validateShortAnswer(saq, number);
        }
    }

    private void validateMultipleChoice(MultipleChoiceQuestion mcq, int number) {
        List<String> distractors = mcq.distractors();
        if (distractors == null || distractors.isEmpty()) {
            throw new InvalidQuizOutputException("Question " + number + " (MULTIPLE_CHOICE) has no distractors");
        }
        if (mcq.correctOption() == null || mcq.correctOption().isBlank()) {
            throw new InvalidQuizOutputException("Question " + number + " (MULTIPLE_CHOICE) has no correctOption");
        }

        Set<String> seen = new HashSet<>();
        for (String distractor : distractors) {
            String key = normalize(distractor);
            if (!seen.add(key)) {
                throw new InvalidQuizOutputException(
                        "Question " + number + " (MULTIPLE_CHOICE) has duplicate distractors: \"" + distractor + "\"");
            }
            if (key.equals(normalize(mcq.correctOption()))) {
                throw new InvalidQuizOutputException(
                        "Question " + number + " (MULTIPLE_CHOICE) has the correctOption among its distractors");
            }
        }
    }

    private void validateMultipleResponse(MultipleResponseQuestion mrq, int number) {
        List<MrqOption> options = mrq.options();
        if (options == null || options.isEmpty()) {
            throw new InvalidQuizOutputException("Question " + number + " (MULTIPLE_RESPONSE) has no options");
        }

        boolean anyCorrect = false;
        Set<String> seen = new HashSet<>();
        for (MrqOption option : options) {
            if (option.text() == null || option.text().isBlank()) {
                throw new InvalidQuizOutputException("Question " + number + " (MULTIPLE_RESPONSE) has an option with no text");
            }
            if (!seen.add(normalize(option.text()))) {
                throw new InvalidQuizOutputException(
                        "Question " + number + " (MULTIPLE_RESPONSE) has duplicate option texts: \"" + option.text() + "\"");
            }
            anyCorrect = anyCorrect || option.isCorrect();
        }
        if (!anyCorrect) {
            throw new InvalidQuizOutputException(
                    "Question " + number + " (MULTIPLE_RESPONSE) has no option marked isCorrect=true");
        }
    }

    private void validateShortAnswer(ShortAnswerQuestion saq, int number) {
        List<String> answers = saq.acceptableAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new InvalidQuizOutputException("Question " + number + " (SHORT_ANSWER) has no acceptableAnswers");
        }

        Set<String> seen = new HashSet<>();
        for (String answer : answers) {
            if (answer == null || answer.isBlank()) {
                throw new InvalidQuizOutputException("Question " + number + " (SHORT_ANSWER) has a blank acceptable answer");
            }
            if (!seen.add(normalize(answer))) {
                throw new InvalidQuizOutputException(
                        "Question " + number + " (SHORT_ANSWER) has duplicate acceptableAnswers: \"" + answer + "\"");
            }
        }
    }


    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
