package com.example.book2quiz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "chapterExecutor")
    public Executor chapterExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("chapter-exec-");
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated executor for the long-running quiz-generation API calls, kept separate
     * from the chapter pipeline so a slow generation never starves chapter conversion.
     */
    @Bean(name = "quizExecutor")
    public Executor quizExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("quiz-exec-");
        executor.initialize();
        return executor;
    }
}
