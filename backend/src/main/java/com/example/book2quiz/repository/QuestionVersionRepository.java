package com.example.book2quiz.repository;

import com.example.book2quiz.model.QuestionVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, Integer> {

    List<QuestionVersion> findByQuestionIdOrderByIdAsc(Integer questionId);

    Optional<QuestionVersion> findByIdAndQuestionId(Integer id, Integer questionId);

    long countByQuestionId(Integer questionId);
}
