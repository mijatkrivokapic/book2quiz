package com.example.book2quiz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A learning objective of a chapter. It owns a list of structural characteristics; each of
 * those belongs to exactly one learning objective. Surface characteristics remain attached
 * directly to the chapter.
 */
@Entity
@Table(name = "learning_objectives")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LearningObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @OneToMany(mappedBy = "learningObjective", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StructuralCharacteristic> structuralCharacteristics = new ArrayList<>();
}
