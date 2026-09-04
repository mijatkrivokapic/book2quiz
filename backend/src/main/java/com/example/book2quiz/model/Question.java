package com.example.book2quiz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A quiz question attached to a chapter. The polymorphic payload (options/distractors/
 * acceptable answers/hints, which vary by type) is stored as JSON in {@code content};
 * {@code questionType} is kept as a column for filtering/display.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private String questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status = QuestionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionOrigin origin = QuestionOrigin.MANUAL;

    /** JSON of the {@code GeneratedQuestion} payload (mirror of the active version). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Id of the active {@link QuestionVersion}; its fields are mirrored above. */
    @Column
    private Integer activeVersionId;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionVersion> versions = new ArrayList<>();

    // Status of an in-progress async regeneration of this question. Null until requested.
    @Enumerated(EnumType.STRING)
    private ProcessingStatus regenerationStatus;

    @Column(columnDefinition = "TEXT")
    private String regenerationError;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /** Copies an active version's fields into this question's mirror. */
    public void mirror(QuestionVersion version) {
        this.activeVersionId = version.getId();
        this.questionType = version.getQuestionType();
        this.status = version.getStatus();
        this.origin = version.getOrigin();
        this.content = version.getContent();
    }
}
