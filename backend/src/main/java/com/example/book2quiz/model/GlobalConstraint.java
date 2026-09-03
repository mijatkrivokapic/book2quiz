package com.example.book2quiz.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "global_constraints")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GlobalConstraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String content;
}
