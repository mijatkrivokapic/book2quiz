package com.example.book2quiz;

import com.example.book2quiz.config.ChapterDetectionProperties;
import com.example.book2quiz.config.CharacteristicProperties;
import com.example.book2quiz.config.MarkdownWorkerProperties;
import com.example.book2quiz.config.QuizProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ChapterDetectionProperties.class, MarkdownWorkerProperties.class,
        QuizProperties.class, CharacteristicProperties.class})
public class Book2quizApplication {

	public static void main(String[] args) {
		SpringApplication.run(Book2quizApplication.class, args);
	}

}
