package com.example.book2quiz.repository;

import com.example.book2quiz.model.SurfaceCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurfaceCharacteristicRepository extends JpaRepository<SurfaceCharacteristic, Integer> {

    List<SurfaceCharacteristic> findByChapterIdOrderByIdAsc(Integer chapterId);

    Optional<SurfaceCharacteristic> findByIdAndChapterId(Integer id, Integer chapterId);
}
