package com.example.book2quiz.service;

import com.example.book2quiz.dto.constraint.ConstraintDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.Constraint;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.ConstraintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD for a chapter's constraints. */
@Service
@Transactional
public class ConstraintService {

    private final ChapterRepository chapterRepository;
    private final ConstraintRepository constraintRepository;

    public ConstraintService(ChapterRepository chapterRepository,
                             ConstraintRepository constraintRepository) {
        this.chapterRepository = chapterRepository;
        this.constraintRepository = constraintRepository;
    }

    @Transactional(readOnly = true)
    public List<ConstraintDTO> list(Integer bookId, int ordinal) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return constraintRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                .map(this::toDTO)
                .toList();
    }

    public ConstraintDTO create(Integer bookId, int ordinal, String content) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        Constraint constraint = new Constraint();
        constraint.setContent(content);
        constraint.setChapter(chapter);
        return toDTO(constraintRepository.save(constraint));
    }

    public ConstraintDTO update(Integer bookId, int ordinal, Integer id, String content) {
        Constraint constraint = findConstraintOrThrow(bookId, ordinal, id);
        constraint.setContent(content);
        return toDTO(constraint);
    }

    public void delete(Integer bookId, int ordinal, Integer id) {
        constraintRepository.delete(findConstraintOrThrow(bookId, ordinal, id));
    }

    private Chapter findChapterOrThrow(Integer bookId, int ordinal) {
        return chapterRepository.findByBookIdAndOrdinal(bookId, ordinal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + ordinal + " not found for book " + bookId));
    }

    private Constraint findConstraintOrThrow(Integer bookId, int ordinal, Integer id) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return constraintRepository.findByIdAndChapterId(id, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Constraint " + id + " not found for chapter " + ordinal + " of book " + bookId));
    }

    private ConstraintDTO toDTO(Constraint constraint) {
        return new ConstraintDTO(constraint.getId(), constraint.getContent());
    }
}
