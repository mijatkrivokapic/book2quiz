package com.example.book2quiz.controller;

import com.example.book2quiz.dto.characteristic.CharacteristicDTO;
import com.example.book2quiz.dto.characteristic.CharacteristicRequestDTO;
import com.example.book2quiz.dto.characteristic.LearningObjectiveDTO;
import com.example.book2quiz.dto.characteristic.LearningObjectiveRequestDTO;
import com.example.book2quiz.dto.characteristic.UpdateCharacteristicStatusRequest;
import com.example.book2quiz.service.LearningObjectiveService;
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
@RequestMapping("/api/books/{bookId}/chapters/{ordinal}/learning-objectives")
public class LearningObjectiveController {

    private final LearningObjectiveService service;

    public LearningObjectiveController(LearningObjectiveService service) {
        this.service = service;
    }

    // ---- Learning objectives ------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<LearningObjectiveDTO>> list(@PathVariable Integer bookId,
                                                           @PathVariable int ordinal) {
        return ResponseEntity.ok(service.list(bookId, ordinal));
    }

    @PostMapping
    public ResponseEntity<LearningObjectiveDTO> create(@PathVariable Integer bookId,
                                                       @PathVariable int ordinal,
                                                       @Valid @RequestBody LearningObjectiveRequestDTO dto) {
        LearningObjectiveDTO created = service.create(bookId, ordinal, dto.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{loId}")
    public ResponseEntity<LearningObjectiveDTO> update(@PathVariable Integer bookId,
                                                       @PathVariable int ordinal,
                                                       @PathVariable Integer loId,
                                                       @Valid @RequestBody LearningObjectiveRequestDTO dto) {
        return ResponseEntity.ok(service.update(bookId, ordinal, loId, dto.description()));
    }

    @PutMapping("/{loId}/status")
    public ResponseEntity<LearningObjectiveDTO> updateStatus(@PathVariable Integer bookId,
                                                             @PathVariable int ordinal,
                                                             @PathVariable Integer loId,
                                                             @Valid @RequestBody UpdateCharacteristicStatusRequest dto) {
        return ResponseEntity.ok(service.updateStatus(bookId, ordinal, loId, dto.status()));
    }

    @DeleteMapping("/{loId}")
    public ResponseEntity<Void> delete(@PathVariable Integer bookId,
                                       @PathVariable int ordinal,
                                       @PathVariable Integer loId) {
        service.delete(bookId, ordinal, loId);
        return ResponseEntity.noContent().build();
    }

    // ---- Structural characteristics (nested under a learning objective) -----------------

    @GetMapping("/{loId}/structural-characteristics")
    public ResponseEntity<List<CharacteristicDTO>> listStructural(@PathVariable Integer bookId,
                                                                  @PathVariable int ordinal,
                                                                  @PathVariable Integer loId) {
        return ResponseEntity.ok(service.listStructural(bookId, ordinal, loId));
    }

    @PostMapping("/{loId}/structural-characteristics")
    public ResponseEntity<CharacteristicDTO> createStructural(@PathVariable Integer bookId,
                                                              @PathVariable int ordinal,
                                                              @PathVariable Integer loId,
                                                              @Valid @RequestBody CharacteristicRequestDTO dto) {
        CharacteristicDTO created = service.createStructural(bookId, ordinal, loId, dto.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{loId}/structural-characteristics/{id}")
    public ResponseEntity<CharacteristicDTO> updateStructural(@PathVariable Integer bookId,
                                                              @PathVariable int ordinal,
                                                              @PathVariable Integer loId,
                                                              @PathVariable Integer id,
                                                              @Valid @RequestBody CharacteristicRequestDTO dto) {
        return ResponseEntity.ok(service.updateStructural(bookId, ordinal, loId, id, dto.content()));
    }

    @PutMapping("/{loId}/structural-characteristics/{id}/status")
    public ResponseEntity<CharacteristicDTO> updateStructuralStatus(@PathVariable Integer bookId,
                                                                    @PathVariable int ordinal,
                                                                    @PathVariable Integer loId,
                                                                    @PathVariable Integer id,
                                                                    @Valid @RequestBody UpdateCharacteristicStatusRequest dto) {
        return ResponseEntity.ok(service.updateStructuralStatus(bookId, ordinal, loId, id, dto.status()));
    }

    @DeleteMapping("/{loId}/structural-characteristics/{id}")
    public ResponseEntity<Void> deleteStructural(@PathVariable Integer bookId,
                                                 @PathVariable int ordinal,
                                                 @PathVariable Integer loId,
                                                 @PathVariable Integer id) {
        service.deleteStructural(bookId, ordinal, loId, id);
        return ResponseEntity.noContent().build();
    }
}
