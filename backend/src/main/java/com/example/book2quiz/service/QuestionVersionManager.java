package com.example.book2quiz.service;

import com.example.book2quiz.model.Question;
import com.example.book2quiz.model.QuestionVersion;
import com.example.book2quiz.repository.QuestionRepository;
import com.example.book2quiz.repository.QuestionVersionRepository;
import org.springframework.stereotype.Component;

/**
 * Shared helper for the question/version relationship, injected by the create paths and
 * the backfill runner (kept dependency-cycle-free: depends only on repositories).
 */
@Component
public class QuestionVersionManager {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository versionRepository;

    public QuestionVersionManager(QuestionRepository questionRepository,
                                  QuestionVersionRepository versionRepository) {
        this.questionRepository = questionRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * Creates the first version of a just-saved question from its current mirror fields
     * (content/type/status/origin) and makes it the active version.
     */
    public void createInitialVersion(Question question) {
        QuestionVersion version = new QuestionVersion();
        version.setQuestion(question);
        version.setContent(question.getContent());
        version.setQuestionType(question.getQuestionType());
        version.setStatus(question.getStatus());
        version.setOrigin(question.getOrigin());
        version = versionRepository.save(version);

        question.setActiveVersionId(version.getId());
        questionRepository.save(question);
    }
}
