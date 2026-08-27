package com.example.book2quiz.controller;

import com.example.book2quiz.dto.constraint.ConstraintDTO;
import com.example.book2quiz.dto.constraint.ConstraintRequestDTO;
import com.example.book2quiz.service.ConstraintService;
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
@RequestMapping("/api/books/{bookId}/chapters/{ordinal}/constraints")
public class ConstraintController {

    private final ConstraintService constraintService;

    public ConstraintController(ConstraintService constraintService) {
        this.constraintService = constraintService;
    }

    @GetMapping
    public ResponseEntity<List<ConstraintDTO>> list(@PathVariable Integer bookId,
                                                    @PathVariable int ordinal) {
        return ResponseEntity.ok(constraintService.list(bookId, ordinal));
    }

    @PostMapping
    public ResponseEntity<ConstraintDTO> create(@PathVariable Integer bookId,
                                                @PathVariable int ordinal,
                                                @Valid @RequestBody ConstraintRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(constraintService.create(bookId, ordinal, dto.content()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConstraintDTO> update(@PathVariable Integer bookId,
                                                @PathVariable int ordinal,
                                                @PathVariable Integer id,
                                                @Valid @RequestBody ConstraintRequestDTO dto) {
        return ResponseEntity.ok(constraintService.update(bookId, ordinal, id, dto.content()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer bookId,
                                       @PathVariable int ordinal,
                                       @PathVariable Integer id) {
        constraintService.delete(bookId, ordinal, id);
        return ResponseEntity.noContent().build();
    }
}
