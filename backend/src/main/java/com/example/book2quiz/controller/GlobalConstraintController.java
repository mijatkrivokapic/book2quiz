package com.example.book2quiz.controller;

import com.example.book2quiz.dto.constraint.ConstraintRequestDTO;
import com.example.book2quiz.dto.constraint.GlobalConstraintDTO;
import com.example.book2quiz.service.GlobalConstraintService;
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
@RequestMapping("/api/global-constraints")
public class GlobalConstraintController {

    private final GlobalConstraintService service;

    public GlobalConstraintController(GlobalConstraintService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GlobalConstraintDTO>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<GlobalConstraintDTO> create(@Valid @RequestBody ConstraintRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto.content()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalConstraintDTO> update(@PathVariable Integer id,
                                                      @Valid @RequestBody ConstraintRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto.content()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
