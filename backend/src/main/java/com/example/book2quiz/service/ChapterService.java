package com.example.book2quiz.service;

import com.example.book2quiz.dto.chapter.GetChapterDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.repository.BookRepository;
import com.example.book2quiz.repository.ChapterRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional
public class ChapterService {

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final FileStorageService fileStorageService;
    private final ChapterPipelineExecutor pipelineExecutor;

    public ChapterService(BookRepository bookRepository,
                          ChapterRepository chapterRepository,
                          FileStorageService fileStorageService,
                          ChapterPipelineExecutor pipelineExecutor) {
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.fileStorageService = fileStorageService;
        this.pipelineExecutor = pipelineExecutor;
    }

    /** Validates the book exists, then launches the async extraction pipeline. */
    public void requestExtraction(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }
        pipelineExecutor.runFullPipeline(bookId);
    }

    /** Validates the chapter exists, then re-runs conversion for it asynchronously. */
    public void requestRetry(Integer bookId, int ordinal) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        pipelineExecutor.reprocessChapter(bookId, chapter.getId());
    }

    @Transactional(readOnly = true)
    public List<GetChapterDTO> getChapters(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }
        return chapterRepository.findByBookIdOrderByOrdinal(bookId).stream()
                .map(this::toGetChapterDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public GetChapterDTO getChapter(Integer bookId, int ordinal) {
        return toGetChapterDTO(findChapterOrThrow(bookId, ordinal));
    }

    /**
     * Overwrites a chapter's Markdown with manually edited content. Writes to the
     * chapter's canonical Markdown key (creating it if the chapter had none yet) and
     * marks the chapter DONE, clearing any prior error.
     */
    public GetChapterDTO updateChapterMarkdown(Integer bookId, int ordinal, String content) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        String key = chapter.getMarkdownObjectKey() != null
                ? chapter.getMarkdownObjectKey()
                : "books/" + bookId + "/chapters/" + ordinal + ".md";

        fileStorageService.uploadBytes(content.getBytes(StandardCharsets.UTF_8), key, "text/markdown");

        chapter.setMarkdownObjectKey(key);
        chapter.setStatus(ProcessingStatus.DONE);
        chapter.setErrorMessage(null);
        return toGetChapterDTO(chapter);
    }

    @Transactional(readOnly = true)
    public InputStreamResource streamChapterMarkdown(Integer bookId, int ordinal) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        if (chapter.getMarkdownObjectKey() == null || chapter.getStatus() != ProcessingStatus.DONE) {
            throw new ResourceNotFoundException(
                    "Markdown not available for chapter " + ordinal + " of book " + bookId);
        }
        return new InputStreamResource(fileStorageService.openStream(chapter.getMarkdownObjectKey()));
    }

    @Transactional(readOnly = true)
    public InputStreamResource streamChapterPdf(Integer bookId, int ordinal) {
        Chapter chapter = findChapterOrThrow(bookId, ordinal);
        return new InputStreamResource(fileStorageService.openStream(chapter.getPdfObjectKey()));
    }

    private Chapter findChapterOrThrow(Integer bookId, int ordinal) {
        return chapterRepository.findByBookIdAndOrdinal(bookId, ordinal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + ordinal + " not found for book " + bookId));
    }

    private GetChapterDTO toGetChapterDTO(Chapter chapter) {
        return new GetChapterDTO(
                chapter.getId(),
                chapter.getOrdinal(),
                chapter.getTitle(),
                chapter.getStartPage(),
                chapter.getEndPage(),
                chapter.getStatus(),
                chapter.getErrorMessage(),
                chapter.getMarkdownObjectKey() != null,
                chapter.getCreatedAt(),
                chapter.getUpdatedAt()
        );
    }
}
