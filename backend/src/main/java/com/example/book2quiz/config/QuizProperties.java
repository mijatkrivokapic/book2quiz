package com.example.book2quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * All quiz-generation configuration. Nothing about the model call is hardcoded — model,
 * limits, timeouts and prompt file locations are bound from {@code quiz.*} properties.
 */
@ConfigurationProperties(prefix = "quiz")
public class QuizProperties {

    /** Identifies the current prompt/schema version; logged with every generation. */
    private String promptVersion = "v1";

    /** Reject a request before calling the API if the combined material exceeds this. */
    private int maxMaterialChars = 100_000;

    private final Anthropic anthropic = new Anthropic();
    private final Prompt prompt = new Prompt();

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public int getMaxMaterialChars() {
        return maxMaterialChars;
    }

    public void setMaxMaterialChars(int maxMaterialChars) {
        this.maxMaterialChars = maxMaterialChars;
    }

    public Anthropic getAnthropic() {
        return anthropic;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public static class Anthropic {
        private String model = "claude-opus-5";
        private long maxTokens = 16_000;

        /**
         * Optional. Omitted when null (the current Opus/Sonnet 5 family rejects
         * temperature with a 400); set it only for models that still accept sampling.
         */
        private Double temperature;

        /** Generous — generation can take a while. */
        private Duration timeout = Duration.ofMinutes(5);

        /** SDK built-in retry (exponential backoff) for 408/409/429/5xx + connection errors. */
        private int maxRetries = 2;

        private final StructuredOutputs structuredOutputs = new StructuredOutputs();

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public long getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(long maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public StructuredOutputs getStructuredOutputs() {
            return structuredOutputs;
        }

        public static class StructuredOutputs {
            /** When true, constrain the response to the JSON schema; else fall back to prompt-only JSON. */
            private boolean enabled = true;

            /** Optional anthropic-beta header value; applied only when non-blank. */
            private String betaHeader = "";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getBetaHeader() {
                return betaHeader;
            }

            public void setBetaHeader(String betaHeader) {
                this.betaHeader = betaHeader;
            }
        }
    }

    public static class Prompt {
        private Resource systemFile = new ClassPathResource("prompts/quiz-system.txt");
        private Resource schemaFile = new ClassPathResource("prompts/quiz-schema.json");

        public Resource getSystemFile() {
            return systemFile;
        }

        public void setSystemFile(Resource systemFile) {
            this.systemFile = systemFile;
        }

        public Resource getSchemaFile() {
            return schemaFile;
        }

        public void setSchemaFile(Resource schemaFile) {
            this.schemaFile = schemaFile;
        }
    }
}
