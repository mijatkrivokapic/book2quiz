package com.example.book2quiz.service;

import com.example.book2quiz.dto.characteristic.CharacteristicDTO;
import com.example.book2quiz.dto.characteristic.CharacteristicGenerationStatusResponse;
import com.example.book2quiz.dto.characteristic.CharacteristicType;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.CharacteristicOrigin;
import com.example.book2quiz.model.CharacteristicStatus;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.model.StructuralCharacteristic;
import com.example.book2quiz.model.SurfaceCharacteristic;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.StructuralCharacteristicRepository;
import com.example.book2quiz.repository.SurfaceCharacteristicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * CRUD, approval and async generation for a chapter's structural and surface
 * characteristics. Both kinds share the same shape, so the type is passed in and used to
 * select the right repository.
 */
@Service
@Transactional
public class ChapterCharacteristicService {

    private final ChapterRepository chapterRepository;
    private final StructuralCharacteristicRepository structuralRepository;
    private final SurfaceCharacteristicRepository surfaceRepository;
    private final CharacteristicGenerationExecutor generationExecutor;

    public ChapterCharacteristicService(ChapterRepository chapterRepository,
                                        StructuralCharacteristicRepository structuralRepository,
                                        SurfaceCharacteristicRepository surfaceRepository,
                                        CharacteristicGenerationExecutor generationExecutor) {
        this.chapterRepository = chapterRepository;
        this.structuralRepository = structuralRepository;
        this.surfaceRepository = surfaceRepository;
        this.generationExecutor = generationExecutor;
    }

    @Transactional(readOnly = true)
    public List<CharacteristicDTO> list(Integer bookId, int ordinal, CharacteristicType type) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return switch (type) {
            case STRUCTURAL -> structuralRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin()))
                    .toList();
            case SURFACE -> surfaceRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin()))
                    .toList();
        };
    }

    public CharacteristicDTO create(Integer bookId, int ordinal, CharacteristicType type, String content) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        // Manually authored characteristics are trusted, so they start APPROVED.
        return switch (type) {
            case STRUCTURAL -> {
                StructuralCharacteristic c = new StructuralCharacteristic();
                c.setContent(content);
                c.setChapter(chapter);
                c.setStatus(CharacteristicStatus.APPROVED);
                c.setOrigin(CharacteristicOrigin.MANUAL);
                c = structuralRepository.save(c);
                yield toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin());
            }
            case SURFACE -> {
                SurfaceCharacteristic c = new SurfaceCharacteristic();
                c.setContent(content);
                c.setChapter(chapter);
                c.setStatus(CharacteristicStatus.APPROVED);
                c.setOrigin(CharacteristicOrigin.MANUAL);
                c = surfaceRepository.save(c);
                yield toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin());
            }
        };
    }

    public CharacteristicDTO update(Integer bookId, int ordinal, CharacteristicType type,
                                    Integer id, String content) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return switch (type) {
            case STRUCTURAL -> {
                StructuralCharacteristic c = structuralRepository.findByIdAndChapterId(id, chapterId)
                        .orElseThrow(() -> notFound(type, id, bookId, ordinal));
                c.setContent(content);
                yield toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin());
            }
            case SURFACE -> {
                SurfaceCharacteristic c = surfaceRepository.findByIdAndChapterId(id, chapterId)
                        .orElseThrow(() -> notFound(type, id, bookId, ordinal));
                c.setContent(content);
                yield toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin());
            }
        };
    }

    public CharacteristicDTO updateStatus(Integer bookId, int ordinal, CharacteristicType type,
                                          Integer id, CharacteristicStatus status) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return switch (type) {
            case STRUCTURAL -> {
                StructuralCharacteristic c = structuralRepository.findByIdAndChapterId(id, chapterId)
                        .orElseThrow(() -> notFound(type, id, bookId, ordinal));
                c.setStatus(status);
                yield toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin());
            }
            case SURFACE -> {
                SurfaceCharacteristic c = surfaceRepository.findByIdAndChapterId(id, chapterId)
                        .orElseThrow(() -> notFound(type, id, bookId, ordinal));
                c.setStatus(status);
                yield toDTO(c.getId(), c.getContent(), c.getStatus(), c.getOrigin());
            }
        };
    }

    public void delete(Integer bookId, int ordinal, CharacteristicType type, Integer id) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        switch (type) {
            case STRUCTURAL -> structuralRepository.delete(
                    structuralRepository.findByIdAndChapterId(id, chapterId)
                            .orElseThrow(() -> notFound(type, id, bookId, ordinal)));
            case SURFACE -> surfaceRepository.delete(
                    surfaceRepository.findByIdAndChapterId(id, chapterId)
                            .orElseThrow(() -> notFound(type, id, bookId, ordinal)));
        }
    }

    /**
     * Validates the chapter can be generated from, marks it PROCESSING, and launches the
     * async generation once the status change has committed. Generates both structural
     * and surface characteristics in one call.
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

    private CharacteristicDTO toDTO(Integer id, String content,
                                    CharacteristicStatus status, CharacteristicOrigin origin) {
        // Legacy rows (created before approval existed) count as manually approved.
        return new CharacteristicDTO(
                id, content,
                status == null ? CharacteristicStatus.APPROVED : status,
                origin == null ? CharacteristicOrigin.MANUAL : origin);
    }

    private Chapter findChapterOrThrow(Integer bookId, int ordinal) {
        return chapterRepository.findByBookIdAndOrdinal(bookId, ordinal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + ordinal + " not found for book " + bookId));
    }

    private ResourceNotFoundException notFound(CharacteristicType type, Integer id, Integer bookId, int ordinal) {
        return new ResourceNotFoundException(
                type.name().toLowerCase() + " characteristic " + id
                        + " not found for chapter " + ordinal + " of book " + bookId);
    }
}
