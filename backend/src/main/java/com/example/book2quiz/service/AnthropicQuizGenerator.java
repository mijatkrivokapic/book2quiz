package com.example.book2quiz.service;

import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.example.book2quiz.dto.quiz.GeneratedQuiz;
import com.example.book2quiz.dto.quiz.QuestionRegenerationRequest;
import com.example.book2quiz.dto.quiz.QuizGenerationRequest;
import com.example.book2quiz.dto.quiz.RegeneratedQuestion;
import com.example.book2quiz.dto.quiz.RegeneratedQuestionResult;
import com.example.book2quiz.dto.quiz.TokenUsage;
import com.example.book2quiz.config.QuizProperties;
import com.example.book2quiz.dto.quiz.QuizGenerationResult;
import com.example.book2quiz.exception.InvalidQuizOutputException;
import com.example.book2quiz.exception.QuizApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * The only class aware of the Anthropic SDK. Builds the request from the configured
 * model settings + prompt layer, calls the Messages API (with the SDK's built-in
 * exponential-backoff retry for 429/5xx), and parses the response into the domain DTOs.
 *
 * <p>Structured outputs constrain the response to the JSON schema when enabled; when
 * disabled the model is relied on to emit plain JSON (per the system prompt) and any
 * markdown fences are stripped before parsing.
 */
@Component
public class AnthropicQuizGenerator implements QuizGenerator {

    private static final Logger log = LoggerFactory.getLogger(AnthropicQuizGenerator.class);

    private final QuizProperties properties;
    private final QuizPromptBuilder promptBuilder;
    private final QuizPromptLoader promptLoader;
    private final AnthropicClientProvider clientProvider;
    private final ObjectMapper objectMapper;

    private final OutputConfig outputConfig;
    private final OutputConfig regenerateOutputConfig;

    public AnthropicQuizGenerator(QuizProperties properties,
                                  QuizPromptBuilder promptBuilder,
                                  QuizPromptLoader promptLoader,
                                  AnthropicClientProvider clientProvider,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.promptLoader = promptLoader;
        this.clientProvider = clientProvider;
        this.objectMapper = objectMapper;
        boolean structured = properties.getAnthropic().getStructuredOutputs().isEnabled();
        this.outputConfig = structured ? buildOutputConfig(properties.getPrompt().getSchemaFile()) : null;
        this.regenerateOutputConfig =
                structured ? buildOutputConfig(properties.getPrompt().getRegenerateSchemaFile()) : null;
    }

    @Override
    public GeneratedQuiz generate(QuizGenerationRequest request) {
        String userMessage = promptBuilder.buildUserMessage(request);
        MessageCreateParams params = buildParams(promptLoader.getSystemPrompt(), userMessage, outputConfig);

        Message response = call(params);

        QuizGenerationResult result = parse(extractText(response), QuizGenerationResult.class, "quiz");
        return new GeneratedQuiz(result.analysis(), result.questions(), usageOf(response));
    }

    @Override
    public RegeneratedQuestion regenerate(QuestionRegenerationRequest request) {
        String userMessage = promptBuilder.buildRegenerationUserMessage(request);
        MessageCreateParams params =
                buildParams(promptLoader.getRegenerateSystemPrompt(), userMessage, regenerateOutputConfig);

        Message response = call(params);

        RegeneratedQuestionResult result = parse(extractText(response), RegeneratedQuestionResult.class, "question");
        return new RegeneratedQuestion(result.analysis(), result.question(), usageOf(response));
    }

    private Message call(MessageCreateParams params) {
        try {
            return clientProvider.get().messages().create(params);
        } catch (AnthropicException e) {
            throw new QuizApiException("Anthropic API call failed: " + e.getMessage(), e);
        }
    }

    private TokenUsage usageOf(Message response) {
        return new TokenUsage(
                properties.getAnthropic().getModel(),
                response.usage().inputTokens(),
                response.usage().outputTokens());
    }

    private MessageCreateParams buildParams(String systemPrompt, String userMessage, OutputConfig config) {
        QuizProperties.Anthropic anthropic = properties.getAnthropic();
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(anthropic.getModel())
                .maxTokens(anthropic.getMaxTokens())
                .system(systemPrompt)
                .addUserMessage(userMessage);

        if (anthropic.getTemperature() != null) {
            builder.temperature(anthropic.getTemperature());
        }

        if (config != null) {
            builder.outputConfig(config);
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
                    "Failed to load quiz JSON schema from " + schemaResource.getDescription(), e);
        }
    }

    private String extractText(Message response) {
        StringBuilder sb = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(text -> sb.append(text.text())));
        return sb.toString();
    }

    private <T> T parse(String raw, Class<T> type, String label) {
        String cleaned = stripMarkdownFences(raw);
        if (cleaned.isBlank()) {
            throw new InvalidQuizOutputException("Model returned an empty response");
        }
        try {
            return objectMapper.readValue(cleaned, type);
        } catch (IOException e) {
            log.debug("Unparseable model output: {}", cleaned);
            throw new InvalidQuizOutputException("Failed to parse model output as a " + label + ": " + e.getMessage(), e);
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
