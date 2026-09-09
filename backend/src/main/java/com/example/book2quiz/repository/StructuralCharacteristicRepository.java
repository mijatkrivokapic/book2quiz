package com.example.book2quiz.repository;

import com.example.book2quiz.model.StructuralCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StructuralCharacteristicRepository extends JpaRepository<StructuralCharacteristic, Integer> {

     List<StructuralCharacteristic> findByLearningObjectiveIdOrderByIdAsc(Integer learningObjectiveId);

    Optional<StructuralCharacteristic> findByIdAndLearningObjectiveId(Integer id, Integer learningObjectiveId);

     List<StructuralCharacteristic> findByLearningObjectiveChapterIdOrderByIdAsc(Integer chapterId);
}
