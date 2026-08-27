package com.example.book2quiz.repository;

import com.example.book2quiz.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Integer> {
}
