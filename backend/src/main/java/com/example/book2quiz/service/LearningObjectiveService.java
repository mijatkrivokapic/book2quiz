package com.example.book2quiz.service;

import com.example.book2quiz.dto.characteristic.CharacteristicDTO;
import com.example.book2quiz.dto.characteristic.LearningObjectiveDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.CharacteristicOrigin;
import com.example.book2quiz.model.CharacteristicStatus;
import com.example.book2quiz.model.LearningObjective;
import com.example.book2quiz.model.StructuralCharacteristic;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.LearningObjectiveRepository;
import com.example.book2quiz.repository.StructuralCharacteristicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for a chapter's learning objectives and, nested under each, its structural
 * characteristics. A learning objective belongs to one chapter; a structural characteristic
 * belongs to one learning objective.
 */
@Service
@Transactional
public class LearningObjectiveService {

    private final ChapterRepository chapterRepository;
    private final LearningObjectiveRepository learningObjectiveRepository;
    private final StructuralCharacteristicRepository structuralRepository;

    public LearningObjectiveService(ChapterRepository chapterRepository,
                                    LearningObjectiveRepository learningObjectiveRepository,
                                    StructuralCharacteristicRepository structuralRepository) {
        this.chapterRepository = chapterRepository;
        this.learningObjectiveRepository = learningObjectiveRepository;
        this.structuralRepository = structuralRepository;
    }

    // ---- Learning objectives ------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LearningObjectiveDTO> list(Integer bookId, int ordinal) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return learningObjectiveRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                .map(this::toDTO)
                .toList();
    }

    public LearningObjectiveDTO create(Integer bookId, int ordinal, String description) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        LearningObjective lo = new LearningObjective();
        lo.setDescription(description);
        lo.setChapter(chapter);
        // Manually authored objectives are trusted, so they start APPROVED.
        lo.setStatus(CharacteristicStatus.APPROVED);
        lo.setOrigin(CharacteristicOrigin.MANUAL);
        return toDTO(learningObjectiveRepository.save(lo));
    }

    public LearningObjectiveDTO update(Integer bookId, int ordinal, Integer loId, String description) {
        LearningObjective lo = findObjectiveOrThrow(bookId, ordinal, loId);
        lo.setDescription(description);
        return toDTO(lo);
    }

    public LearningObjectiveDTO updateStatus(Integer bookId, int ordinal, Integer loId, CharacteristicStatus status) {
        LearningObjective lo = findObjectiveOrThrow(bookId, ordinal, loId);
        lo.setStatus(status);
        return toDTO(lo);
    }

    public void delete(Integer bookId, int ordinal, Integer loId) {
        learningObjectiveRepository.delete(findObjectiveOrThrow(bookId, ordinal, loId));
    }

    // ---- Structural characteristics (nested under a learning objective) -----------------

    @Transactional(readOnly = true)
    public List<CharacteristicDTO> listStructural(Integer bookId, int ordinal, Integer loId) {
        findObjectiveOrThrow(bookId, ordinal, loId);
        return structuralRepository.findByLearningObjectiveIdOrderByIdAsc(loId).stream()
                .map(this::toDTO)
                .toList();
    }

    public CharacteristicDTO createStructural(Integer bookId, int ordinal, Integer loId, String content) {
        LearningObjective lo = findObjectiveOrThrow(bookId, ordinal, loId);
        StructuralCharacteristic c = new StructuralCharacteristic();
        c.setContent(content);
        c.setLearningObjective(lo);
        // Manually authored characteristics are trusted, so they start APPROVED.
        c.setStatus(CharacteristicStatus.APPROVED);
        c.setOrigin(CharacteristicOrigin.MANUAL);
        return toDTO(structuralRepository.save(c));
    }

    public CharacteristicDTO updateStructural(Integer bookId, int ordinal, Integer loId,
                                              Integer id, String content) {
        StructuralCharacteristic c = findStructuralOrThrow(bookId, ordinal, loId, id);
        c.setContent(content);
        return toDTO(c);
    }

    public CharacteristicDTO updateStructuralStatus(Integer bookId, int ordinal, Integer loId,
                                                    Integer id, CharacteristicStatus status) {
        StructuralCharacteristic c = findStructuralOrThrow(bookId, ordinal, loId, id);
        c.setStatus(status);
        return toDTO(c);
    }

    public void deleteStructural(Integer bookId, int ordinal, Integer loId, Integer id) {
        structuralRepository.delete(findStructuralOrThrow(bookId, ordinal, loId, id));
    }

    // ---- Helpers ------------------------------------------------------------------------

    private LearningObjectiveDTO toDTO(LearningObjective lo) {
        // Legacy rows (created before approval existed) count as manually approved.
        return new LearningObjectiveDTO(
                lo.getId(), lo.getDescription(),
                lo.getStatus() == null ? CharacteristicStatus.APPROVED : lo.getStatus(),
                lo.getOrigin() == null ? CharacteristicOrigin.MANUAL : lo.getOrigin());
    }

    private CharacteristicDTO toDTO(StructuralCharacteristic c) {
        // Legacy rows (created before approval existed) count as manually approved.
        return new CharacteristicDTO(
                c.getId(), c.getContent(),
                c.getStatus() == null ? CharacteristicStatus.APPROVED : c.getStatus(),
                c.getOrigin() == null ? CharacteristicOrigin.MANUAL : c.getOrigin());
    }

    private Chapter findChapterOrThrow(Integer bookId, int ordinal) {
        return chapterRepository.findByBookIdAndOrdinal(bookId, ordinal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + ordinal + " not found for book " + bookId));
    }

    private LearningObjective findObjectiveOrThrow(Integer bookId, int ordinal, Integer loId) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return learningObjectiveRepository.findByIdAndChapterId(loId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Learning objective " + loId + " not found for chapter " + ordinal + " of book " + bookId));
    }

    private StructuralCharacteristic findStructuralOrThrow(Integer bookId, int ordinal, Integer loId, Integer id) {
        findObjectiveOrThrow(bookId, ordinal, loId);
        return structuralRepository.findByIdAndLearningObjectiveId(id, loId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Structural characteristic " + id + " not found for learning objective " + loId));
    }
}
