package com.example.book2quiz.controller;

import com.example.book2quiz.dto.characteristic.CharacteristicDTO;
import com.example.book2quiz.dto.characteristic.CharacteristicGenerationStatusResponse;
import com.example.book2quiz.dto.characteristic.CharacteristicRequestDTO;
import com.example.book2quiz.dto.characteristic.CharacteristicType;
import com.example.book2quiz.dto.characteristic.UpdateCharacteristicStatusRequest;
import com.example.book2quiz.service.ChapterCharacteristicService;
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
@RequestMapping("/api/books/{bookId}/chapters/{ordinal}/characteristics")
public class ChapterCharacteristicController {

    private final ChapterCharacteristicService service;

    public ChapterCharacteristicController(ChapterCharacteristicService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generate(@PathVariable Integer bookId, @PathVariable int ordinal) {
        service.requestGeneration(bookId, ordinal);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/generation-status")
    public ResponseEntity<CharacteristicGenerationStatusResponse> generationStatus(@PathVariable Integer bookId,
                                                                                   @PathVariable int ordinal) {
        return ResponseEntity.ok(service.getGenerationStatus(bookId, ordinal));
    }

    @GetMapping("/{type}")
    public ResponseEntity<List<CharacteristicDTO>> list(@PathVariable Integer bookId,
                                                        @PathVariable int ordinal,
                                                        @PathVariable String type) {
        return ResponseEntity.ok(service.list(bookId, ordinal, parseType(type)));
    }

    @PostMapping("/{type}")
    public ResponseEntity<CharacteristicDTO> create(@PathVariable Integer bookId,
                                                    @PathVariable int ordinal,
                                                    @PathVariable String type,
                                                    @Valid @RequestBody CharacteristicRequestDTO dto) {
        CharacteristicDTO created = service.create(bookId, ordinal, parseType(type), dto.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{type}/{id}")
    public ResponseEntity<CharacteristicDTO> update(@PathVariable Integer bookId,
                                                    @PathVariable int ordinal,
                                                    @PathVariable String type,
                                                    @PathVariable Integer id,
                                                    @Valid @RequestBody CharacteristicRequestDTO dto) {
        return ResponseEntity.ok(service.update(bookId, ordinal, parseType(type), id, dto.content()));
    }

    @PutMapping("/{type}/{id}/status")
    public ResponseEntity<CharacteristicDTO> updateStatus(@PathVariable Integer bookId,
                                                          @PathVariable int ordinal,
                                                          @PathVariable String type,
                                                          @PathVariable Integer id,
                                                          @Valid @RequestBody UpdateCharacteristicStatusRequest dto) {
        return ResponseEntity.ok(service.updateStatus(bookId, ordinal, parseType(type), id, dto.status()));
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer bookId,
                                       @PathVariable int ordinal,
                                       @PathVariable String type,
                                       @PathVariable Integer id) {
        service.delete(bookId, ordinal, parseType(type), id);
        return ResponseEntity.noContent().build();
    }

    private CharacteristicType parseType(String type) {
        try {
            return CharacteristicType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown characteristic type: " + type);
        }
    }
}
