package com.example.book2quiz.controller;

import com.example.book2quiz.dto.generation.GenerationRecordDTO;
import com.example.book2quiz.dto.generation.GenerationUsageSummaryDTO;
import com.example.book2quiz.service.GenerationRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}/chapters/{ordinal}/generation-records")
public class GenerationRecordController {

    private final GenerationRecordService service;

    public GenerationRecordController(GenerationRecordService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GenerationRecordDTO>> list(@PathVariable Integer bookId,
                                                          @PathVariable int ordinal) {
        return ResponseEntity.ok(service.list(bookId, ordinal));
    }

    @GetMapping("/summary")
    public ResponseEntity<GenerationUsageSummaryDTO> summary(@PathVariable Integer bookId,
                                                             @PathVariable int ordinal) {
        return ResponseEntity.ok(service.summary(bookId, ordinal));
    }
}
