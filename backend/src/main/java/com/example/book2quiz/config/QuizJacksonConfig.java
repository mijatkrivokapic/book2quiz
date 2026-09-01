package com.example.book2quiz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuizJacksonConfig {

    @Bean
    public ObjectMapper quizObjectMapper() {
        return new ObjectMapper();
    }
}
