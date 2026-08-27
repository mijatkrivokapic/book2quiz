package com.example.book2quiz.repository;

import com.example.book2quiz.model.StructuralCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StructuralCharacteristicRepository extends JpaRepository<StructuralCharacteristic, Integer> {

    List<StructuralCharacteristic> findByChapterIdOrderByIdAsc(Integer chapterId);

    Optional<StructuralCharacteristic> findByIdAndChapterId(Integer id, Integer chapterId);
}
