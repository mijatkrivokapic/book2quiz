package com.example.book2quiz.controller;

import com.example.book2quiz.service.GenerationEventPublisher;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE stream of async-job status changes for one book (chapter extraction, characteristic /
 * question / regeneration generation). The frontend opens one stream per book instead of
 * polling status endpoints.
 */
@RestController
@RequestMapping("/api/books/{bookId}")
public class GenerationEventController {

    private final GenerationEventPublisher publisher;

    public GenerationEventController(GenerationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable int bookId, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return publisher.subscribe(bookId);
    }
}
