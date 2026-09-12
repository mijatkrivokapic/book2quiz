package com.example.book2quiz.service;

import com.example.book2quiz.dto.generation.GenerationEvent;
import com.example.book2quiz.model.Book;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.repository.BookRepository;
import com.example.book2quiz.repository.ChapterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the chapter extraction / conversion pipeline asynchronously on the dedicated
 * {@code chapterExecutor}. Chapters are processed one at a time; each step updates
 * the database so progress is observable via the chapter list endpoint. A failure of
 * one chapter does not abort the others.
 */
@Component
public class ChapterPipelineExecutor {

    private static final Logger log = LoggerFactory.getLogger(ChapterPipelineExecutor.class);

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterExtractionService extractionService;
    private final MarkdownWorkerClient workerClient;
    private final FileStorageService fileStorageService;
    private final GenerationEventPublisher events;

    public ChapterPipelineExecutor(BookRepository bookRepository,
                                   ChapterRepository chapterRepository,
                                   ChapterExtractionService extractionService,
                                   MarkdownWorkerClient workerClient,
                                   FileStorageService fileStorageService,
                                   GenerationEventPublisher events) {
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.extractionService = extractionService;
        this.workerClient = workerClient;
        this.fileStorageService = fileStorageService;
        this.events = events;
    }

    /**
     * Full pipeline for a book: detect + split chapters, then convert each to Markdown.
     * Any previously extracted chapters for the book are discarded first.
     */
    @Async("chapterExecutor")
    public void runFullPipeline(int bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            log.warn("Book {} no longer exists, aborting chapter extraction", bookId);
            return;
        }

        log.info("Book {}: starting chapter extraction pipeline", bookId);
        book.setChapterExtractionStatus(ProcessingStatus.PROCESSING);
        book.setChapterDetectionReviewNeeded(false);
        bookRepository.save(book);
        chapterRepository.deleteByBookId(bookId);

        List<Chapter> chapters;
        try {
            ChapterExtractionService.ExtractionResult result = extractionService.extractAndSplit(book);

            chapters = new ArrayList<>();
            int ordinal = 1;
            for (ChapterExtractionService.ExtractedChapter ec : result.chapters()) {
                Chapter chapter = new Chapter();
                chapter.setBook(book);
                chapter.setOrdinal(ordinal++);
                chapter.setTitle(ec.title());
                chapter.setStartPage(ec.startPage());
                chapter.setEndPage(ec.endPage());
                chapter.setPdfObjectKey(ec.pdfObjectKey());
                chapter.setStatus(ProcessingStatus.PENDING);
                chapters.add(chapter);
            }
            chapters = chapterRepository.saveAll(chapters);

            if (result.detectionFailed()) {
                book.setChapterDetectionReviewNeeded(true);
                bookRepository.save(book);
            }
        } catch (Exception e) {
            log.error("Book {}: chapter detection/splitting failed", bookId, e);
            book.setChapterExtractionStatus(ProcessingStatus.FAILED);
            bookRepository.save(book);
            events.publish(bookId, GenerationEvent.extraction(ProcessingStatus.FAILED));
            return;
        }

        for (Chapter chapter : chapters) {
            convertChapter(bookId, chapter.getId());
        }
        finalizeBookStatus(bookId);
        log.info("Book {}: chapter extraction pipeline finished", bookId);
    }

    /**
     * Re-runs Markdown conversion for a single, already-split chapter.
     */
    @Async("chapterExecutor")
    public void reprocessChapter(int bookId, int chapterId) {
        log.info("Book {}: retrying chapter {}", bookId, chapterId);
        convertChapter(bookId, chapterId);
        finalizeBookStatus(bookId);
    }

    private void convertChapter(int bookId, int chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null) {
            log.warn("Book {}: chapter {} vanished before conversion", bookId, chapterId);
            return;
        }

        chapter.setStatus(ProcessingStatus.PROCESSING);
        chapter.setErrorMessage(null);
        chapterRepository.save(chapter);
        events.publish(bookId, GenerationEvent.chapter(chapter.getOrdinal(), ProcessingStatus.PROCESSING, null));

        try {
            log.info("Book {}: converting chapter {} (ordinal {}) from {}",
                    bookId, chapterId, chapter.getOrdinal(), chapter.getPdfObjectKey());

            String markdown = workerClient.convert(fileStorageService.getBucketName(), chapter.getPdfObjectKey());

            String markdownKey = "books/" + bookId + "/chapters/" + chapter.getOrdinal() + ".md";
            fileStorageService.uploadBytes(
                    markdown.getBytes(StandardCharsets.UTF_8), markdownKey, "text/markdown");

            chapter.setMarkdownObjectKey(markdownKey);
            chapter.setStatus(ProcessingStatus.DONE);
            chapter.setErrorMessage(null);
            chapterRepository.save(chapter);
            events.publish(bookId, GenerationEvent.chapter(chapter.getOrdinal(), ProcessingStatus.DONE, null));
            log.info("Book {}: chapter {} (ordinal {}) DONE -> {}",
                    bookId, chapterId, chapter.getOrdinal(), markdownKey);
        } catch (Exception e) {
            log.error("Book {}: chapter {} (ordinal {}) conversion FAILED",
                    bookId, chapterId, chapter.getOrdinal(), e);
            chapter.setStatus(ProcessingStatus.FAILED);
            chapter.setErrorMessage(truncate(e.getMessage()));
            chapterRepository.save(chapter);
            events.publish(bookId, GenerationEvent.chapter(chapter.getOrdinal(), ProcessingStatus.FAILED,
                    chapter.getErrorMessage()));
        }
    }

    private void finalizeBookStatus(int bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return;
        }
        List<Chapter> chapters = chapterRepository.findByBookIdOrderByOrdinal(bookId);
        boolean anyFailed = chapters.stream().anyMatch(c -> c.getStatus() == ProcessingStatus.FAILED);
        boolean anyPending = chapters.stream()
                .anyMatch(c -> c.getStatus() == ProcessingStatus.PENDING
                        || c.getStatus() == ProcessingStatus.PROCESSING);

        ProcessingStatus bookStatus = anyFailed
                ? ProcessingStatus.FAILED
                : (anyPending ? ProcessingStatus.PROCESSING : ProcessingStatus.DONE);
        book.setChapterExtractionStatus(bookStatus);
        bookRepository.save(book);
        events.publish(bookId, GenerationEvent.extraction(bookStatus));
        log.info("Book {}: chapter extraction status = {}", bookId, bookStatus);
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
