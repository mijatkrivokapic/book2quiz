package com.example.book2quiz.service;

import com.example.book2quiz.config.QuizProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads the static system prompt from a configurable Spring {@link Resource} at startup.
 * Fails fast with a clear error if the file is missing or empty. (Global constraints now
 * live in the database and are managed from the app.)
 */
@Component
public class QuizPromptLoader {

    private final String systemPrompt;

    public QuizPromptLoader(QuizProperties properties) {
        this.systemPrompt = readNonEmpty(properties.getPrompt().getSystemFile(), "quiz.prompt.system-file");
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    private String readNonEmpty(Resource resource, String property) {
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException(
                    "Prompt file for '" + property + "' does not exist: " + describe(resource));
        }
        String content = readAll(resource, property).strip();
        if (content.isEmpty()) {
            throw new IllegalStateException("Prompt file for '" + property + "' is empty: " + describe(resource));
        }
        return content;
    }

    private String readAll(Resource resource, String property) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt file for '" + property + "': " + describe(resource), e);
        }
    }

    private String describe(Resource resource) {
        return resource == null ? "<null>" : resource.getDescription();
    }
}
