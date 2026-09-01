package com.example.book2quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Characteristic-generation configuration. The Anthropic model/client settings are
 * shared with {@link QuizProperties} (same account and model); only the prompt/schema
 * and limits are configured here.
 */
@ConfigurationProperties(prefix = "characteristics")
public class CharacteristicProperties {

    /** Identifies the current prompt/schema version; logged with every generation. */
    private String promptVersion = "v1";

    /** Reject a request before calling the API if the combined material exceeds this. */
    private int maxMaterialChars = 100_000;

    private Resource systemFile = new ClassPathResource("prompts/characteristics-system.txt");
    private Resource schemaFile = new ClassPathResource("prompts/characteristics-schema.json");

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
