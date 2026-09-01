package com.example.book2quiz.service;

import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.example.book2quiz.config.CharacteristicProperties;
import com.example.book2quiz.config.QuizProperties;
import com.example.book2quiz.dto.characteristic.CharacteristicGenerationRequest;
import com.example.book2quiz.dto.characteristic.CharacteristicGenerationResult;
import com.example.book2quiz.dto.characteristic.GeneratedCharacteristics;
import com.example.book2quiz.dto.quiz.TokenUsage;
import com.example.book2quiz.exception.InvalidQuizOutputException;
import com.example.book2quiz.exception.QuizApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/**
 * Anthropic adapter for characteristic generation. Uses the shared model settings and
 * client, its own system prompt + JSON schema, and a user message that appends
 * <strong>only</strong> the {@code <instructional_items>} section.
 */
@Component
public class AnthropicCharacteristicGenerator implements CharacteristicGenerator {

    private static final Logger log = LoggerFactory.getLogger(AnthropicCharacteristicGenerator.class);

    private final QuizProperties quizProperties;
    private final CharacteristicProperties properties;
    private final AnthropicClientProvider clientProvider;
    private final ObjectMapper objectMapper;

    private final String systemPrompt;
    private final OutputConfig outputConfig;

    public AnthropicCharacteristicGenerator(QuizProperties quizProperties,
                                            CharacteristicProperties properties,
                                            AnthropicClientProvider clientProvider,
                                            ObjectMapper objectMapper) {
        this.quizProperties = quizProperties;
        this.properties = properties;
        this.clientProvider = clientProvider;
        this.objectMapper = objectMapper;
        this.systemPrompt = readSystemPrompt(properties.getSystemFile());
        this.outputConfig = quizProperties.getAnthropic().getStructuredOutputs().isEnabled()
                ? buildOutputConfig(properties.getSchemaFile())
                : null;
    }

    @Override
    public GeneratedCharacteristics generate(CharacteristicGenerationRequest request) {
        // Only instructional items are appended for this prompt.
        String userMessage = XmlSection.of(
                "instructional_items", "instructional_item", request.instructionalItemsOrEmpty());

        MessageCreateParams params = buildParams(userMessage);

        Message response;
        try {
            response = clientProvider.get().messages().create(params);
        } catch (AnthropicException e) {
            throw new QuizApiException("Anthropic API call failed: " + e.getMessage(), e);
        }

        CharacteristicGenerationResult result = parse(extractText(response));
        TokenUsage usage = new TokenUsage(
                quizProperties.getAnthropic().getModel(),
                response.usage().inputTokens(),
                response.usage().outputTokens());
        return new GeneratedCharacteristics(
                result.analysis(), result.structuralCharacteristics(), result.surfaceCharacteristics(), usage);
    }

    private MessageCreateParams buildParams(String userMessage) {
        QuizProperties.Anthropic anthropic = quizProperties.getAnthropic();
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(anthropic.getModel())
                .maxTokens(anthropic.getMaxTokens())
                .system(systemPrompt)
                .addUserMessage(userMessage);

        if (anthropic.getTemperature() != null) {
            builder.temperature(anthropic.getTemperature());
        }
        if (outputConfig != null) {
            builder.outputConfig(outputConfig);
            String beta = anthropic.getStructuredOutputs().getBetaHeader();
            if (beta != null && !beta.isBlank()) {
                builder.putAdditionalHeader("anthropic-beta", beta);
            }
        }
        return builder.build();
    }

    private OutputConfig buildOutputConfig(Resource schemaResource) {
        try {
            JsonNode schema = objectMapper.readTree(schemaResource.getInputStream());
            JsonOutputFormat.Schema.Builder schemaBuilder = JsonOutputFormat.Schema.builder();
            Iterator<Map.Entry<String, JsonNode>> fields = schema.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                schemaBuilder.putAdditionalProperty(field.getKey(), JsonValue.fromJsonNode(field.getValue()));
            }
            return OutputConfig.builder()
                    .format(JsonOutputFormat.builder().schema(schemaBuilder.build()).build())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load characteristic JSON schema from " + schemaResource.getDescription(), e);
        }
    }

    private String readSystemPrompt(Resource resource) {
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException("Characteristic system prompt not found: "
                    + (resource == null ? "<null>" : resource.getDescription()));
        }
        try {
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (content.isEmpty()) {
                throw new IllegalStateException("Characteristic system prompt is empty: " + resource.getDescription());
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read characteristic system prompt: " + resource.getDescription(), e);
        }
    }

    private String extractText(Message response) {
        StringBuilder sb = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(text -> sb.append(text.text())));
        return sb.toString();
    }

    private CharacteristicGenerationResult parse(String raw) {
        String cleaned = stripMarkdownFences(raw);
        if (cleaned.isBlank()) {
            throw new InvalidQuizOutputException("Model returned an empty response");
        }
        try {
            return objectMapper.readValue(cleaned, CharacteristicGenerationResult.class);
        } catch (IOException e) {
            log.debug("Unparseable model output: {}", cleaned);
            throw new InvalidQuizOutputException("Failed to parse model output as characteristics: " + e.getMessage(), e);
        }
    }

    private String stripMarkdownFences(String raw) {
        String text = raw.strip();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline >= 0) {
            text = text.substring(firstNewline + 1);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.strip();
    }
}
