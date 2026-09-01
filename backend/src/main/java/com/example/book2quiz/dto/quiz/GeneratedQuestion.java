package com.example.book2quiz.dto.quiz;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * A generated quiz question. The concrete shape is selected by the
 * {@code questionType} discriminator, which Jackson reads/writes but which is not
 * a component of the records themselves.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "questionType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MultipleResponseQuestion.class, name = "MULTIPLE_RESPONSE"),
        @JsonSubTypes.Type(value = MultipleChoiceQuestion.class, name = "MULTIPLE_CHOICE"),
        @JsonSubTypes.Type(value = ShortAnswerQuestion.class, name = "SHORT_ANSWER")
})
public sealed interface GeneratedQuestion
        permits MultipleResponseQuestion, MultipleChoiceQuestion, ShortAnswerQuestion {

    String text();

    List<String> hints();

    /** The discriminator value written to {@code questionType}. */
    default String questionType() {
        return switch (this) {
            case MultipleResponseQuestion ignored -> "MULTIPLE_RESPONSE";
            case MultipleChoiceQuestion ignored -> "MULTIPLE_CHOICE";
            case ShortAnswerQuestion ignored -> "SHORT_ANSWER";
        };
    }
}
