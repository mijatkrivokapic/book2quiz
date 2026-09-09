package com.example.book2quiz.repository;

import com.example.book2quiz.model.LearningObjective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningObjectiveRepository extends JpaRepository<LearningObjective, Integer> {

    List<LearningObjective> findByChapterIdOrderByIdAsc(Integer chapterId);

    Optional<LearningObjective> findByIdAndChapterId(Integer id, Integer chapterId);
}
