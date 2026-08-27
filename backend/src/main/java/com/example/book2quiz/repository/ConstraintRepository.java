package com.example.book2quiz.repository;

import com.example.book2quiz.model.Constraint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConstraintRepository extends JpaRepository<Constraint, Integer> {

    List<Constraint> findByChapterIdOrderByIdAsc(Integer chapterId);

    Optional<Constraint> findByIdAndChapterId(Integer id, Integer chapterId);
}
