package com.example.book2quiz.service;

import com.anthropic.models.messages.CacheControlEphemeral;
import com.example.book2quiz.config.QuizProperties;

/** Helpers for building the ephemeral {@code cache_control} marker from the cache config. */
final class PromptCache {

    private PromptCache() {
    }

    /** The ephemeral cache-control marker with the configured TTL. */
    static CacheControlEphemeral ephemeral(QuizProperties.Anthropic.Cache cache) {
        return CacheControlEphemeral.builder().ttl(ttl(cache.getTtl())).build();
    }

    private static CacheControlEphemeral.Ttl ttl(String value) {
        if (value == null) {
            return CacheControlEphemeral.Ttl.TTL_1H;
        }
        return switch (value.trim().toLowerCase()) {
            case "5m" -> CacheControlEphemeral.Ttl.TTL_5M;
            case "1h" -> CacheControlEphemeral.Ttl.TTL_1H;
            default -> throw new IllegalArgumentException(
                    "Unknown quiz.anthropic.cache.ttl: '" + value + "' (expected 5m or 1h)");
        };
    }
}
