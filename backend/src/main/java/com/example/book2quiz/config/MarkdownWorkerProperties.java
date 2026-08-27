package com.example.book2quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Connection and retry settings for the external Python (Docling) Markdown worker.
 */
@ConfigurationProperties(prefix = "markdown-worker")
public class MarkdownWorkerProperties {

    /**
     * Base URL of the worker, e.g. http://markdown-worker:8000
     */
    private String baseUrl = "http://markdown-worker:8000";

    /**
     * Timeout for establishing the connection to the worker.
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Timeout for reading the response. Conversion (with OCR) can take minutes, so
     * this is deliberately generous.
     */
    private Duration readTimeout = Duration.ofMinutes(10);

    /**
     * Number of retries after the initial attempt fails (total attempts = retries + 1).
     */
    private int retries = 2;

    /**
     * Base backoff between retries; doubled on each subsequent retry.
     */
    private Duration retryBackoff = Duration.ofSeconds(5);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }
}
