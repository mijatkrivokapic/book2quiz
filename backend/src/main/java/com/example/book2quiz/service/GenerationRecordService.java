package com.example.book2quiz.service;

import com.example.book2quiz.dto.generation.GenerationRecordDTO;
import com.example.book2quiz.dto.generation.GenerationUsageSummaryDTO;
import com.example.book2quiz.dto.quiz.TokenUsage;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.GenerationKind;
import com.example.book2quiz.model.GenerationRecord;
import com.example.book2quiz.repository.ChapterRepository;
import com.example.book2quiz.repository.GenerationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Persists and reads the per-chapter generation/token-usage history. Each successful LLM
 * generation (characteristics, questions, or a single-question regeneration) records what
 * was produced, the model and prompt version, the token consumption and the duration.
 */
@Service
@Transactional
public class GenerationRecordService {

    private static final Logger log = LoggerFactory.getLogger(GenerationRecordService.class);

    private final ChapterRepository chapterRepository;
    private final GenerationRecordRepository recordRepository;

    public GenerationRecordService(ChapterRepository chapterRepository,
                                   GenerationRecordRepository recordRepository) {
        this.chapterRepository = chapterRepository;
        this.recordRepository = recordRepository;
    }

    /**
     * Records one generation's token usage against a chapter. Never throws into the caller:
     * a failure to record history must not fail the generation itself.
     */
    public void record(int chapterId, GenerationKind type, TokenUsage usage,
                       String promptVersion, long durationMs, String summary) {
        try {
            GenerationRecord entry = new GenerationRecord();
            entry.setChapter(chapterRepository.getReferenceById(chapterId));
            entry.setType(type);
            entry.setModel(usage == null ? null : usage.model());
            entry.setPromptVersion(promptVersion);
            long input = usage == null ? 0 : usage.inputTokens();
            long output = usage == null ? 0 : usage.outputTokens();
            entry.setInputTokens(input);
            entry.setOutputTokens(output);
            entry.setTotalTokens(input + output);
            entry.setCacheCreationInputTokens(usage == null ? 0 : usage.cacheCreationInputTokens());
            entry.setCacheReadInputTokens(usage == null ? 0 : usage.cacheReadInputTokens());
            entry.setDurationMs(durationMs);
            entry.setSummary(summary);
            recordRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to record generation history for chapter {} ({}): {}",
                    chapterId, type, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<GenerationRecordDTO> list(Integer bookId, int ordinal) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        return recordRepository.findByChapterIdOrderByCreatedAtDescIdDesc(chapterId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public GenerationUsageSummaryDTO summary(Integer bookId, int ordinal) {
        int chapterId = findChapterOrThrow(bookId, ordinal).getId();
        List<GenerationRecord> records = recordRepository.findByChapterIdOrderByCreatedAtDescIdDesc(chapterId);

        long totalInput = 0;
        long totalOutput = 0;
        long totalCacheCreation = 0;
        long totalCacheRead = 0;
        Map<GenerationKind, long[]> acc = new EnumMap<>(GenerationKind.class); // [count, input, output, cacheCreation, cacheRead]
        for (GenerationRecord r : records) {
            long cacheCreation = nullToZero(r.getCacheCreationInputTokens());
            long cacheRead = nullToZero(r.getCacheReadInputTokens());
            totalInput += r.getInputTokens();
            totalOutput += r.getOutputTokens();
            totalCacheCreation += cacheCreation;
            totalCacheRead += cacheRead;
            long[] a = acc.computeIfAbsent(r.getType(), k -> new long[5]);
            a[0] += 1;
            a[1] += r.getInputTokens();
            a[2] += r.getOutputTokens();
            a[3] += cacheCreation;
            a[4] += cacheRead;
        }

        Map<GenerationKind, GenerationUsageSummaryDTO.TypeUsage> byType = new EnumMap<>(GenerationKind.class);
        acc.forEach((type, a) -> byType.put(type,
                new GenerationUsageSummaryDTO.TypeUsage((int) a[0], a[1], a[2], a[1] + a[2], a[3], a[4])));

        return new GenerationUsageSummaryDTO(
                records.size(), totalInput, totalOutput, totalInput + totalOutput,
                totalCacheCreation, totalCacheRead, byType);
    }

    private GenerationRecordDTO toDTO(GenerationRecord r) {
        return new GenerationRecordDTO(
                r.getId(), r.getType(), r.getModel(), r.getPromptVersion(),
                r.getInputTokens(), r.getOutputTokens(), r.getTotalTokens(),
                nullToZero(r.getCacheCreationInputTokens()), nullToZero(r.getCacheReadInputTokens()),
                r.getDurationMs(), r.getSummary(), r.getCreatedAt());
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private Chapter findChapterOrThrow(Integer bookId, int ordinal) {
        return chapterRepository.findByBookIdAndOrdinal(bookId, ordinal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + ordinal + " not found for book " + bookId));
    }
}
