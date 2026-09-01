package com.example.book2quiz.service;

import com.example.book2quiz.config.QuizProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class QuizPromptLoader {

    private final String systemPrompt;
    private final List<String> baseConstraints;

    public QuizPromptLoader(QuizProperties properties) {
        QuizProperties.Prompt prompt = properties.getPrompt();
        this.systemPrompt = readNonEmpty(prompt.getSystemFile(), "quiz.prompt.system-file");
        this.baseConstraints = readConstraints(prompt.getBaseConstraintsFile());
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public List<String> getBaseConstraints() {
        return baseConstraints;
    }

    private String readNonEmpty(Resource resource, String property) {
        String content = readAll(resource, property).strip();
        if (content.isEmpty()) {
            throw new IllegalStateException("Prompt file for '" + property + "' is empty: " + describe(resource));
        }
        return content;
    }

    /** Trims each line; skips blank lines and lines starting with '#'. */
    private List<String> readConstraints(Resource resource) {
        String raw = readAll(resource, "quiz.prompt.base-constraints-file");
        List<String> constraints = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            constraints.add(trimmed);
        }
        if (constraints.isEmpty()) {
            throw new IllegalStateException(
                    "Base constraints file has no usable constraints (all blank/comments): " + describe(resource));
        }
        return List.copyOf(constraints);
    }

    private String readAll(Resource resource, String property) {
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException(
                    "Prompt file for '" + property + "' does not exist: " + describe(resource));
        }
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
