package com.example.book2quiz.service;

import com.example.book2quiz.config.CharacteristicProperties;
import com.example.book2quiz.dto.characteristic.CharacteristicGenerationRequest;
import com.example.book2quiz.dto.characteristic.GeneratedCharacteristics;
import com.example.book2quiz.dto.characteristic.GeneratedLearningObjective;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.CharacteristicOrigin;
import com.example.book2quiz.model.CharacteristicStatus;
import com.example.book2quiz.model.GenerationKind;
import com.example.book2quiz.model.LearningObjective;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.model.StructuralCharacteristic;
import com.example.book2quiz.model.SurfaceCharacteristic;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.LearningObjectiveRepository;
import com.example.book2quiz.repository.SurfaceCharacteristicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Runs characteristic generation on the {@code quizExecutor}. Assembles the chapter's
 * material, calls the generator, persists the learning objectives (each with its structural
 * characteristics) and the surface characteristics as GENERATED / PENDING, and records the
 * outcome on the chapter's {@code characteristicGenerationStatus} so the UI can poll for
 * progress.
 */
@Component
public class CharacteristicGenerationExecutor {

    private static final Logger log = LoggerFactory.getLogger(CharacteristicGenerationExecutor.class);

    private final ChapterRepository chapterRepository;
    private final LearningObjectiveRepository learningObjectiveRepository;
    private final SurfaceCharacteristicRepository surfaceRepository;
    private final FileStorageService fileStorageService;
    private final CharacteristicGenerationService generationService;
    private final GenerationRecordService generationRecordService;
    private final CharacteristicProperties characteristicProperties;

    public CharacteristicGenerationExecutor(ChapterRepository chapterRepository,
                                            LearningObjectiveRepository learningObjectiveRepository,
                                            SurfaceCharacteristicRepository surfaceRepository,
                                            FileStorageService fileStorageService,
                                            CharacteristicGenerationService generationService,
                                            GenerationRecordService generationRecordService,
                                            CharacteristicProperties characteristicProperties) {
        this.chapterRepository = chapterRepository;
        this.learningObjectiveRepository = learningObjectiveRepository;
        this.surfaceRepository = surfaceRepository;
        this.fileStorageService = fileStorageService;
        this.generationService = generationService;
        this.generationRecordService = generationRecordService;
        this.characteristicProperties = characteristicProperties;
    }

    @Async("quizExecutor")
    public void runGeneration(int bookId, int ordinal) {
        Chapter chapter = chapterRepository.findByBookIdAndOrdinal(bookId, ordinal).orElse(null);
        if (chapter == null) {
            log.warn("Book {} chapter {}: vanished before characteristic generation", bookId, ordinal);
            return;
        }
        int chapterId = chapter.getId();

        try {
            String material = new String(
                    fileStorageService.downloadFile(chapter.getMarkdownObjectKey()), StandardCharsets.UTF_8);
            log.info("Book {} chapter {}: generating characteristics ({} chars material)", bookId, ordinal, material.length());

            long start = System.currentTimeMillis();
            GeneratedCharacteristics result =
                    generationService.generate(new CharacteristicGenerationRequest(List.of(material)));
            long durationMs = System.currentTimeMillis() - start;

            persist(chapterId, result);
            int structuralCount = result.learningObjectives().stream()
                    .mapToInt(lo -> lo.structuralCharacteristics() == null ? 0 : lo.structuralCharacteristics().size())
                    .sum();
            generationRecordService.record(chapterId, GenerationKind.CHARACTERISTICS, result.usage(),
                    characteristicProperties.getPromptVersion(), durationMs,
                    result.learningObjectives().size() + " objectives, " + structuralCount + " structural, "
                            + result.surfaceCharacteristics().size() + " surface");
            finish(chapterId, ProcessingStatus.DONE, null);
            log.info("Book {} chapter {}: generated {} learning objective(s), {} structural + {} surface characteristic(s)",
                    bookId, ordinal, result.learningObjectives().size(), structuralCount,
                    result.surfaceCharacteristics().size());
        } catch (Exception e) {
            log.error("Book {} chapter {}: characteristic generation failed", bookId, ordinal, e);
            finish(chapterId, ProcessingStatus.FAILED, truncate(e.getMessage()));
        }
    }

    private void persist(int chapterId, GeneratedCharacteristics result) {
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow();

        // Each generated learning objective is persisted with its structural characteristics
        // (cascaded through the objective). Both start GENERATED / PENDING for review.
        for (GeneratedLearningObjective genObjective : result.learningObjectives()) {
            if (genObjective == null
                    || genObjective.description() == null || genObjective.description().isBlank()) {
                continue;
            }
            LearningObjective objective = new LearningObjective();
            objective.setChapter(chapter);
            objective.setDescription(genObjective.description().strip());
            objective.setOrigin(CharacteristicOrigin.GENERATED);
            objective.setStatus(CharacteristicStatus.PENDING);

            List<String> structural = genObjective.structuralCharacteristics();
            if (structural != null) {
                for (String content : structural) {
                    if (content == null || content.isBlank()) {
                        continue;
                    }
                    StructuralCharacteristic c = new StructuralCharacteristic();
                    c.setLearningObjective(objective);
                    c.setContent(content.strip());
                    c.setOrigin(CharacteristicOrigin.GENERATED);
                    c.setStatus(CharacteristicStatus.PENDING);
                    objective.getStructuralCharacteristics().add(c);
                }
            }
            learningObjectiveRepository.save(objective);
        }

        for (String content : result.surfaceCharacteristics()) {
            if (content == null || content.isBlank()) {
                continue;
            }
            SurfaceCharacteristic c = new SurfaceCharacteristic();
            c.setChapter(chapter);
            c.setContent(content.strip());
            c.setOrigin(CharacteristicOrigin.GENERATED);
            c.setStatus(CharacteristicStatus.PENDING);
            surfaceRepository.save(c);
        }
    }

    private void finish(int chapterId, ProcessingStatus status, String error) {
        chapterRepository.findById(chapterId).ifPresent(chapter -> {
            chapter.setCharacteristicGenerationStatus(status);
            chapter.setCharacteristicGenerationError(error);
            chapterRepository.save(chapter);
        });
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
