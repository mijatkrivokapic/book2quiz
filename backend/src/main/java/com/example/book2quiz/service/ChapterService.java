package com.example.book2quiz.service;

import com.example.book2quiz.dto.chapter.ChapterSource;
import com.example.book2quiz.dto.chapter.CreateChapterDTO;
import com.example.book2quiz.dto.chapter.GetChapterDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.Book;
import com.example.book2quiz.model.Chapter;
import com.example.book2quiz.model.ProcessingStatus;
import com.example.book2quiz.repository.BookRepository;
import com.example.book2quiz.repository.ChapterRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    /**
     * Manually creates a chapter and appends it to the book. For {@link ChapterSource#PDF}
     * the uploaded PDF is stored and converted to Markdown asynchronously (chapter starts
     * PENDING); for {@link ChapterSource#MARKDOWN} the content is stored directly (DONE).
     */
    public GetChapterDTO createChapter(Integer bookId, CreateChapterDTO dto) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        int ordinal = nextOrdinal(bookId);

        Chapter chapter = new Chapter();
        chapter.setBook(book);
        chapter.setOrdinal(ordinal);
        chapter.setTitle(dto.title());
        chapter.setStartPage(0);
        chapter.setEndPage(0);

        if (dto.source() == ChapterSource.MARKDOWN) {
            String content = dto.content();
            if (content == null) {
                throw new IllegalArgumentException("Markdown content is required");
            }
            String markdownKey = "books/" + bookId + "/chapters/" + ordinal + ".md";
            fileStorageService.uploadBytes(content.getBytes(StandardCharsets.UTF_8), markdownKey, "text/markdown");
            chapter.setMarkdownObjectKey(markdownKey);
            chapter.setStatus(ProcessingStatus.DONE);
            return toGetChapterDTO(chapterRepository.save(chapter));
        }

        // ChapterSource.PDF
        MultipartFile file = dto.file();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required");
        }
        String pdfKey = "books/" + bookId + "/chapters/" + ordinal + ".pdf";
        fileStorageService.uploadBytes(readBytes(file), pdfKey, "application/pdf");
        chapter.setPdfObjectKey(pdfKey);
        chapter.setStatus(ProcessingStatus.PENDING);

        Chapter saved = chapterRepository.save(chapter);

        // Convert only after the new row is committed, so the async task can read it.
        int chapterId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pipelineExecutor.reprocessChapter(bookId, chapterId);
            }
        });

        return toGetChapterDTO(saved);
    }

    private int nextOrdinal(Integer bookId) {
        return chapterRepository.findByBookIdOrderByOrdinal(bookId).stream()
                .mapToInt(Chapter::getOrdinal)
                .max()
                .orElse(0) + 1;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
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
        if (chapter.getPdfObjectKey() == null) {
            throw new ResourceNotFoundException(
                    "No PDF available for chapter " + ordinal + " of book " + bookId);
        }
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
                chapter.getPdfObjectKey() != null,
                chapter.getCreatedAt(),
                chapter.getUpdatedAt()
        );
    }
}
