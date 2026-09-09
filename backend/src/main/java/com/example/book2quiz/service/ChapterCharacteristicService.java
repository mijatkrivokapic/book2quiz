package com.example.book2quiz.service;

import com.example.book2quiz.dto.characteristic.CharacteristicDTO;
import com.example.book2quiz.dto.characteristic.CharacteristicGenerationStatusResponse;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.CharacteristicOrigin;
import com.example.book2quiz.model.CharacteristicStatus;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.model.SurfaceCharacteristic;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.SurfaceCharacteristicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * CRUD, approval and async generation for a chapter's surface characteristics. Structural
 * characteristics are managed per learning objective in {@link LearningObjectiveService};
 * this service also owns the (shared) generation trigger and status for the chapter.
 */
@Service
@Transactional
public class ChapterCharacteristicService {

    private final ChapterRepository chapterRepository;
    private final SurfaceCharacteristicRepository surfaceRepository;
    private final CharacteristicGenerationExecutor generationExecutor;

    public ChapterCharacteristicService(ChapterRepository chapterRepository,
                                        SurfaceCharacteristicRepository surfaceRepository,
                                        CharacteristicGenerationExecutor generationExecutor) {
        this.chapterRepository = chapterRepository;
        this.surfaceRepository = surfaceRepository;
        this.generationExecutor = generationExecutor;
    }

    @Transactional(readOnly = true)
    public List<CharacteristicDTO> listSurface(Integer bookId, int ordinal) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return surfaceRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                .map(this::toDTO)
                .toList();
    }

    public CharacteristicDTO createSurface(Integer bookId, int ordinal, String content) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        SurfaceCharacteristic c = new SurfaceCharacteristic();
        c.setContent(content);
        c.setChapter(chapter);
        // Manually authored characteristics are trusted, so they start APPROVED.
        c.setStatus(CharacteristicStatus.APPROVED);
        c.setOrigin(CharacteristicOrigin.MANUAL);
        return toDTO(surfaceRepository.save(c));
    }

    public CharacteristicDTO updateSurface(Integer bookId, int ordinal, Integer id, String content) {
        SurfaceCharacteristic c = findSurfaceOrThrow(bookId, ordinal, id);
        c.setContent(content);
        return toDTO(c);
    }

    public CharacteristicDTO updateSurfaceStatus(Integer bookId, int ordinal, Integer id,
                                                 CharacteristicStatus status) {
        SurfaceCharacteristic c = findSurfaceOrThrow(bookId, ordinal, id);
        c.setStatus(status);
        return toDTO(c);
    }

    public void deleteSurface(Integer bookId, int ordinal, Integer id) {
        surfaceRepository.delete(findSurfaceOrThrow(bookId, ordinal, id));
    }

    /**
     * Validates the chapter can be generated from, marks it PROCESSING, and launches the
     * async generation once the status change has committed.
     */
    public void requestGeneration(Integer bookId, int ordinal) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        if (chapter.getMarkdownObjectKey() == null) {
            throw new IllegalArgumentException(
                    "Chapter " + ordinal + " has no Markdown yet; convert it before generating characteristics");
        }
        if (chapter.getCharacteristicGenerationStatus() == ProcessingStatus.PROCESSING) {
            throw new IllegalArgumentException("Characteristic generation is already in progress for this chapter");
        }

        chapter.setCharacteristicGenerationStatus(ProcessingStatus.PROCESSING);
        chapter.setCharacteristicGenerationError(null);

        int bId = bookId;
        int ord = ordinal;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                generationExecutor.runGeneration(bId, ord);
            }
        });
    }

    @Transactional(readOnly = true)
    public CharacteristicGenerationStatusResponse getGenerationStatus(Integer bookId, int ordinal) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        return new CharacteristicGenerationStatusResponse(
                chapter.getCharacteristicGenerationStatus(), chapter.getCharacteristicGenerationError());
    }

    private CharacteristicDTO toDTO(SurfaceCharacteristic c) {
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

    private SurfaceCharacteristic findSurfaceOrThrow(Integer bookId, int ordinal, Integer id) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return surfaceRepository.findByIdAndChapterId(id, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "surface characteristic " + id + " not found for chapter " + ordinal + " of book " + bookId));
    }
}
