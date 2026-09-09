package com.example.book2quiz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * One entry in a chapter's generation/token-usage history: what was generated
 * (characteristics, questions, or a single-question regeneration), when, with which model,
 * and the token consumption for that call.
 */
@Entity
@Table(name = "generation_records")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GenerationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationKind type;

    @Column(nullable = false)
    private String model;

    // Version of the prompt/schema used for this generation (from the generator config).
    @Column
    private String promptVersion;

    @Column(nullable = false)
    private long inputTokens;

    @Column(nullable = false)
    private long outputTokens;

    @Column(nullable = false)
    private long totalTokens;

    // Prompt-caching activity: tokens written to / served from the cache for this call.
    // Nullable so schema auto-update tolerates rows created before caching was added.
    @Column
    private Long cacheCreationInputTokens;

    @Column
    private Long cacheReadInputTokens;

    // Wall-clock duration of the generation call, in milliseconds.
    @Column(nullable = false)
    private long durationMs;

    // Short human-readable detail (e.g. "5 questions", "4 objectives, 18 structural, 15 surface").
    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
