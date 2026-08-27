package com.example.book2quiz.controller;

import com.example.book2quiz.dto.chapter.GetChapterDTO;
import com.example.book2quiz.dto.chapter.UpdateChapterMarkdownDTO;
import com.example.book2quiz.service.ChapterService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/extract-chapters")
    public ResponseEntity<Void> extractChapters(@PathVariable Integer bookId) {
        chapterService.requestExtraction(bookId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/chapters/{ordinal}/retry")
    public ResponseEntity<Void> retryChapter(@PathVariable Integer bookId, @PathVariable int ordinal) {
        chapterService.requestRetry(bookId, ordinal);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/chapters")
    public ResponseEntity<List<GetChapterDTO>> getChapters(@PathVariable Integer bookId) {
        return ResponseEntity.ok(chapterService.getChapters(bookId));
    }

    @GetMapping("/chapters/{ordinal}")
    public ResponseEntity<GetChapterDTO> getChapter(@PathVariable Integer bookId, @PathVariable int ordinal) {
        return ResponseEntity.ok(chapterService.getChapter(bookId, ordinal));
    }

    @GetMapping("/chapters/{ordinal}/markdown")
    public ResponseEntity<InputStreamResource> getChapterMarkdown(@PathVariable Integer bookId,
                                                                  @PathVariable int ordinal) {
        InputStreamResource body = chapterService.streamChapterMarkdown(bookId, ordinal);
        //TODO use presigned url?
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown"))
                .body(body);
    }

    @PutMapping("/chapters/{ordinal}/markdown")
    public ResponseEntity<GetChapterDTO> updateChapterMarkdown(@PathVariable Integer bookId,
                                                               @PathVariable int ordinal,
                                                               @Valid @RequestBody UpdateChapterMarkdownDTO dto) {
        return ResponseEntity.ok(chapterService.updateChapterMarkdown(bookId, ordinal, dto.content()));
    }

    @GetMapping("/chapters/{ordinal}/pdf")
    public ResponseEntity<InputStreamResource> getChapterPdf(@PathVariable Integer bookId,
                                                             @PathVariable int ordinal) {
        InputStreamResource body = chapterService.streamChapterPdf(bookId, ordinal);
        //TODO use presigned url?
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"book-" + bookId + "-chapter-" + ordinal + ".pdf\"")
                .body(body);
    }
}
