package com.example.book2quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Tunables for the heuristic (no-outline) chapter detection. The patterns are
 * matched against text lines; a line only counts as a chapter start when it also
 * renders in a font at least {@code fontSizeRatio} times the body font size.
 */
@ConfigurationProperties(prefix = "chapter-detection")
public class ChapterDetectionProperties {

    /**
     * Regular expressions identifying candidate chapter heading lines. Matched
     * case-insensitively against the trimmed line text.
     */
    private List<String> patterns = List.of(
            "^chapter\\s+\\d+.*",
            "^poglavlje\\s+\\d+.*",
            "^\\d+\\.\\s+\\S.*"
    );

    /**
     * A heading line's font size must be at least this multiple of the detected
     * body font size to qualify as a chapter start.
     */
    private double fontSizeRatio = 1.3;

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public double getFontSizeRatio() {
        return fontSizeRatio;
    }

    public void setFontSizeRatio(double fontSizeRatio) {
        this.fontSizeRatio = fontSizeRatio;
    }
}
