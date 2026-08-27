package com.example.book2quiz.dto.chapter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Manual chapter creation. When {@code source} is PDF a {@code file} must be
 * supplied (it is converted to Markdown by the worker); when MARKDOWN the
 * {@code content} must be supplied. Sent as multipart/form-data.
 */
public record CreateChapterDTO(
        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Source is required")
        ChapterSource source,

        MultipartFile file,

        String content
) {
}
