package com.example.book2quiz.repository;

import com.example.book2quiz.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Integer> {
}
