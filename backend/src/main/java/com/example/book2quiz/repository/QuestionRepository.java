package com.example.book2quiz.repository;

import com.example.book2quiz.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByChapterIdOrderByIdAsc(Integer chapterId);

    Optional<Question> findByIdAndChapterId(Integer id, Integer chapterId);
}
