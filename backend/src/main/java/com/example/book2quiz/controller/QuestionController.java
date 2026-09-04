package com.example.book2quiz.controller;

import com.example.book2quiz.dto.question.QuestionGenerationStatusResponse;
import com.example.book2quiz.dto.question.QuestionResponse;
import com.example.book2quiz.dto.question.QuestionVersionDTO;
import com.example.book2quiz.dto.question.RegenerateQuestionRequest;
import com.example.book2quiz.dto.question.SaveQuestionRequest;
import com.example.book2quiz.dto.question.UpdateQuestionStatusRequest;
import com.example.book2quiz.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}/chapters/{ordinal}/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> list(@PathVariable Integer bookId,
                                                       @PathVariable int ordinal) {
        return ResponseEntity.ok(questionService.list(bookId, ordinal));
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> create(@PathVariable Integer bookId,
                                                   @PathVariable int ordinal,
                                                   @Valid @RequestBody SaveQuestionRequest request) {
        QuestionResponse created = questionService.create(bookId, ordinal, request.question());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generate(@PathVariable Integer bookId, @PathVariable int ordinal) {
        questionService.requestGeneration(bookId, ordinal);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/generation-status")
    public ResponseEntity<QuestionGenerationStatusResponse> generationStatus(@PathVariable Integer bookId,
                                                                             @PathVariable int ordinal) {
        return ResponseEntity.ok(questionService.getGenerationStatus(bookId, ordinal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> update(@PathVariable Integer bookId,
                                                   @PathVariable int ordinal,
                                                   @PathVariable Integer id,
                                                   @Valid @RequestBody SaveQuestionRequest request) {
        return ResponseEntity.ok(questionService.update(bookId, ordinal, id, request.question()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<QuestionResponse> updateStatus(@PathVariable Integer bookId,
                                                         @PathVariable int ordinal,
                                                         @PathVariable Integer id,
                                                         @Valid @RequestBody UpdateQuestionStatusRequest request) {
        return ResponseEntity.ok(questionService.updateStatus(bookId, ordinal, id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer bookId,
                                       @PathVariable int ordinal,
                                       @PathVariable Integer id) {
        questionService.delete(bookId, ordinal, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<Void> regenerate(@PathVariable Integer bookId,
                                           @PathVariable int ordinal,
                                           @PathVariable Integer id,
                                           @RequestBody(required = false) RegenerateQuestionRequest request) {
        String guideline = request == null ? null : request.guideline();
        questionService.requestRegeneration(bookId, ordinal, id, guideline);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<QuestionVersionDTO>> versions(@PathVariable Integer bookId,
                                                             @PathVariable int ordinal,
                                                             @PathVariable Integer id) {
        return ResponseEntity.ok(questionService.listVersions(bookId, ordinal, id));
    }

    @PutMapping("/{id}/versions/{versionId}/activate")
    public ResponseEntity<QuestionResponse> activateVersion(@PathVariable Integer bookId,
                                                            @PathVariable int ordinal,
                                                            @PathVariable Integer id,
                                                            @PathVariable Integer versionId) {
        return ResponseEntity.ok(questionService.activateVersion(bookId, ordinal, id, versionId));
    }

    @PutMapping("/{id}/versions/{versionId}")
    public ResponseEntity<QuestionVersionDTO> updateVersion(@PathVariable Integer bookId,
                                                            @PathVariable int ordinal,
                                                            @PathVariable Integer id,
                                                            @PathVariable Integer versionId,
                                                            @Valid @RequestBody SaveQuestionRequest request) {
        return ResponseEntity.ok(questionService.updateVersion(bookId, ordinal, id, versionId, request.question()));
    }

    @DeleteMapping("/{id}/versions/{versionId}")
    public ResponseEntity<Void> deleteVersion(@PathVariable Integer bookId,
                                              @PathVariable int ordinal,
                                              @PathVariable Integer id,
                                              @PathVariable Integer versionId) {
        questionService.deleteVersion(bookId, ordinal, id, versionId);
        return ResponseEntity.noContent().build();
    }
}
