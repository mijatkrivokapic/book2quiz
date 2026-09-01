package com.example.book2quiz.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.example.book2quiz.config.QuizProperties;
import com.example.book2quiz.exception.QuizApiException;
import org.springframework.stereotype.Component;

/**
 * Lazily builds and caches a single Anthropic client, shared by every generator. The
 * API key comes exclusively from the {@code ANTHROPIC_API_KEY} environment variable, and
 * the retry/timeout come from {@link QuizProperties}. Building it lazily means the app
 * still starts without a key — only an actual generation call fails, with a clear error.
 */
@Component
public class AnthropicClientProvider {

    private static final String API_KEY_ENV = "ANTHROPIC_API_KEY";

    private final QuizProperties properties;
    private volatile AnthropicClient client;

    public AnthropicClientProvider(QuizProperties properties) {
        this.properties = properties;
    }

    public AnthropicClient get() {
        AnthropicClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    String apiKey = System.getenv(API_KEY_ENV);
                    if (apiKey == null || apiKey.isBlank()) {
                        throw new QuizApiException(API_KEY_ENV + " environment variable is not set");
                    }
                    QuizProperties.Anthropic anthropic = properties.getAnthropic();
                    local = AnthropicOkHttpClient.builder()
                            .apiKey(apiKey)
                            .maxRetries(anthropic.getMaxRetries())
                            .timeout(anthropic.getTimeout())
                            .build();
                    client = local;
                }
            }
        }
        return local;
    }
}
