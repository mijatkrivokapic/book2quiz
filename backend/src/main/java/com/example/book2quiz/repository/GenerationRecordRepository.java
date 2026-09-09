package com.example.book2quiz.repository;

import com.example.book2quiz.model.GenerationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerationRecordRepository extends JpaRepository<GenerationRecord, Integer> {

    // Newest first: the history reads best in reverse-chronological order.
    List<GenerationRecord> findByChapterIdOrderByCreatedAtDescIdDesc(Integer chapterId);
}
