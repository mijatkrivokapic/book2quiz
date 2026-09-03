package com.example.book2quiz.repository;

import com.example.book2quiz.model.GlobalConstraint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlobalConstraintRepository extends JpaRepository<GlobalConstraint, Integer> {

    List<GlobalConstraint> findAllByOrderByIdAsc();

    boolean existsByContent(String content);

    boolean existsByContentAndIdNot(String content, Integer id);
}
