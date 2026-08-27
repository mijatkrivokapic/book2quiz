package com.example.book2quiz.repository;

import com.example.book2quiz.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    List<Chapter> findByBookIdOrderByOrdinal(int bookId);

    Optional<Chapter> findByBookIdAndOrdinal(int bookId, int ordinal);

    @Transactional
    void deleteByBookId(int bookId);
}
