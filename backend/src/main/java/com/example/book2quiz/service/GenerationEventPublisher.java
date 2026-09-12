package com.example.book2quiz.service;

import com.example.book2quiz.dto.generation.GenerationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory hub for Server-Sent Events, keyed by book id. Frontend views subscribe once per
 * book; the async executors publish a {@link GenerationEvent} whenever a job's status changes.
 * {@link SseEmitter#send} is thread-safe, so publishing from executor threads is fine.
 */
@Component
public class GenerationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GenerationEventPublisher.class);

    /** Long-lived stream; EventSource auto-reconnects when it lapses. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Integer, Set<SseEmitter>> emittersByBook = new ConcurrentHashMap<>();

    public SseEmitter subscribe(int bookId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByBook.computeIfAbsent(bookId, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> remove(bookId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(bookId, emitter);
        });
        emitter.onError(e -> remove(bookId, emitter));

        // Open the stream immediately (flush headers, defeat proxy buffering).
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            remove(bookId, emitter);
        }
        return emitter;
    }

    public void publish(int bookId, GenerationEvent event) {
        Set<SseEmitter> set = emittersByBook.get(bookId);
        if (set == null || set.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event().data(event));
            } catch (Exception e) {
                log.debug("Dropping SSE emitter for book {}: {}", bookId, e.getMessage());
                remove(bookId, emitter);
            }
        }
    }

    private void remove(int bookId, SseEmitter emitter) {
        Set<SseEmitter> set = emittersByBook.get(bookId);
        if (set != null) {
            set.remove(emitter);
        }
    }
}
