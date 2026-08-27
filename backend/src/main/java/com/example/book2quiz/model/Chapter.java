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

@Entity
@Table(name = "chapters")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /**
     * 1-based position of the chapter within the book. Also used as {n} in the
     * MinIO object keys (books/{bookId}/chapters/{n}.pdf and .md).
     */
    @Column(nullable = false)
    private int ordinal;

    @Column(nullable = false)
    private String title;

    // startPage/endPage are 0 for manually-created chapters that do not map to a
    // page range of the book PDF.
    @Column(nullable = false)
    private int startPage;

    @Column(nullable = false)
    private int endPage;

    // Null for chapters authored directly as Markdown (no source PDF).
    @Column
    private String pdfObjectKey;

    @Column
    private String markdownObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StructuralCharacteristic> structuralCharacteristics = new ArrayList<>();

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurfaceCharacteristic> surfaceCharacteristics = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
