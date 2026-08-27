package com.example.book2quiz.service;

import com.example.book2quiz.dto.characteristic.CharacteristicDTO;
import com.example.book2quiz.dto.characteristic.CharacteristicType;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.StructuralCharacteristic;
import com.example.book2quiz.model.SurfaceCharacteristic;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.StructuralCharacteristicRepository;
import com.example.book2quiz.repository.SurfaceCharacteristicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for a chapter's structural and surface characteristics. Both kinds share
 * the same shape, so the type is passed in and used to select the right repository.
 */
@Service
@Transactional
public class ChapterCharacteristicService {

    private final ChapterRepository chapterRepository;
    private final StructuralCharacteristicRepository structuralRepository;
    private final SurfaceCharacteristicRepository surfaceRepository;

    public ChapterCharacteristicService(ChapterRepository chapterRepository,
                                        StructuralCharacteristicRepository structuralRepository,
                                        SurfaceCharacteristicRepository surfaceRepository) {
        this.chapterRepository = chapterRepository;
        this.structuralRepository = structuralRepository;
        this.surfaceRepository = surfaceRepository;
    }

    @Transactional(readOnly = true)
    public List<CharacteristicDTO> list(Integer bookId, int ordinal, CharacteristicType type) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return switch (type) {
            case STRUCTURAL -> structuralRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> new CharacteristicDTO(c.getId(), c.getContent()))
                    .toList();
            case SURFACE -> surfaceRepository.findByChapterIdOrderByIdAsc(chapterId).stream()
                    .map(c -> new CharacteristicDTO(c.getId(), c.getContent()))
                    .toList();
        };
    }

    public CharacteristicDTO create(Integer bookId, int ordinal, CharacteristicType type, String content) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        return switch (type) {
            case STRUCTURAL -> {
                StructuralCharacteristic c = new StructuralCharacteristic();
                c.setContent(content);
                c.setChapter(chapter);
                c = structuralRepository.save(c);
                yield new CharacteristicDTO(c.getId(), c.getContent());
            }
            case SURFACE -> {
                SurfaceCharacteristic c = new SurfaceCharacteristic();
                c.setContent(content);
                c.setChapter(chapter);
                c = surfaceRepository.save(c);
                yield new CharacteristicDTO(c.getId(), c.getContent());
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
                yield new CharacteristicDTO(c.getId(), c.getContent());
            }
            case SURFACE -> {
                SurfaceCharacteristic c = surfaceRepository.findByIdAndChapterId(id, chapterId)
                        .orElseThrow(() -> notFound(type, id, bookId, ordinal));
                c.setContent(content);
                yield new CharacteristicDTO(c.getId(), c.getContent());
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
