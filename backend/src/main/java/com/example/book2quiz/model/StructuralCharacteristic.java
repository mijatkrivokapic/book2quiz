package com.example.book2quiz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "structural_characteristics")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StructuralCharacteristic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Nullable so schema auto-update tolerates existing rows; the app always sets them,
    // and legacy rows (null) are treated as APPROVED/MANUAL when mapped.
    @Enumerated(EnumType.STRING)
    private CharacteristicStatus status;

    @Enumerated(EnumType.STRING)
    private CharacteristicOrigin origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_objective_id")
    private LearningObjective learningObjective;
}
