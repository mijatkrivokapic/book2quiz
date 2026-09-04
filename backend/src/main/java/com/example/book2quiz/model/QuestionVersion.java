package com.example.book2quiz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * One concrete version of a {@link Question}. A question keeps a history of versions
 * (the original plus each regenerated one); exactly one is the active version, whose
 * fields are mirrored onto the {@link Question} for backward-compatible reads.
 *
 * <p>This is the source of truth for the polymorphic payload ({@code content}),
 * {@code questionType}, review {@code status} and {@code origin}.
 */
@Entity
@Table(name = "question_versions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status = QuestionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionOrigin origin = QuestionOrigin.MANUAL;

    /** JSON of the {@code GeneratedQuestion} payload. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** The regeneration guideline that produced this version; null for the original. */
    @Column(columnDefinition = "TEXT")
    private String guideline;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
