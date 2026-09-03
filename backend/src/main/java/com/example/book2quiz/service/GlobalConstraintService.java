package com.example.book2quiz.service;

import com.example.book2quiz.dto.constraint.GlobalConstraintDTO;
import com.example.book2quiz.exception.ResourceNotFoundException;
import com.example.book2quiz.model.GlobalConstraint;
import com.example.book2quiz.repository.GlobalConstraintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD for application-wide (global/base) quiz constraints, stored in the database. */
@Service
@Transactional
public class GlobalConstraintService {

    private final GlobalConstraintRepository repository;

    public GlobalConstraintService(GlobalConstraintRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GlobalConstraintDTO> list() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    /** Ordered constraint texts, for the prompt builder. */
    @Transactional(readOnly = true)
    public List<String> getContents() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(GlobalConstraint::getContent)
                .toList();
    }

    public GlobalConstraintDTO create(String content) {
        String value = normalize(content);
        if (repository.existsByContent(value)) {
            throw new IllegalArgumentException("A global constraint with this content already exists");
        }
        GlobalConstraint constraint = new GlobalConstraint();
        constraint.setContent(value);
        return toDTO(repository.save(constraint));
    }

    public GlobalConstraintDTO update(Integer id, String content) {
        GlobalConstraint constraint = findOrThrow(id);
        String value = normalize(content);
        if (repository.existsByContentAndIdNot(value, id)) {
            throw new IllegalArgumentException("A global constraint with this content already exists");
        }
        constraint.setContent(value);
        return toDTO(constraint);
    }

    public void delete(Integer id) {
        repository.delete(findOrThrow(id));
    }

    private GlobalConstraint findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Global constraint not found with id: " + id));
    }

    private String normalize(String content) {
        return content == null ? "" : content.strip();
    }

    private GlobalConstraintDTO toDTO(GlobalConstraint constraint) {
        return new GlobalConstraintDTO(constraint.getId(), constraint.getContent());
    }
}
