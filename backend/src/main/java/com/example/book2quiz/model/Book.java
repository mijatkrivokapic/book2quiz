package com.example.book2quiz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String fileKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // Left nullable at the DB level so schema auto-update can add the column to an
    // existing books table; the app always populates it (default PENDING).
    @Enumerated(EnumType.STRING)
    private ProcessingStatus chapterExtractionStatus = ProcessingStatus.PENDING;

    /**
     * Set when automatic chapter detection found no boundaries and the whole book
     * was emitted as a single chapter, so the split can be reviewed manually.
     */
    private boolean chapterDetectionReviewNeeded = false;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinal ASC")
    private List<Chapter> chapters = new ArrayList<>();

    public void addChapter(Chapter chapter) {
        chapters.add(chapter);
        chapter.setBook(this);
    }

}