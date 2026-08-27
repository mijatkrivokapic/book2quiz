package com.example.book2quiz.service;

import com.example.book2quiz.config.MarkdownWorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the external Python (Docling) worker to convert a chapter PDF into Markdown.
 * The worker downloads the PDF itself from MinIO given the bucket + object key, so
 * only the key travels over the wire. Applies a configurable number of retries with
 * exponential backoff around the (potentially minutes-long) conversion call.
 */
@Component
public class MarkdownWorkerClient {

    private static final Logger log = LoggerFactory.getLogger(MarkdownWorkerClient.class);

    private final RestClient restClient;
    private final MarkdownWorkerProperties properties;

    public MarkdownWorkerClient(MarkdownWorkerProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    private record ConvertRequest(String bucket, String objectKey) {
    }

    private record ConvertResponse(String markdown) {
    }

    /**
     * Converts the object at {@code bucket/objectKey} to Markdown, retrying on failure.
     *
     * @throws RuntimeException if every attempt fails
     */
    public String convert(String bucket, String objectKey) {
        int maxAttempts = properties.getRetries() + 1;
        long backoffMillis = properties.getRetryBackoff().toMillis();
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Requesting Markdown conversion of {} (attempt {}/{})", objectKey, attempt, maxAttempts);
                ConvertResponse response = restClient.post()
                        .uri("/convert")
                        .body(new ConvertRequest(bucket, objectKey))
                        .retrieve()
                        .body(ConvertResponse.class);

                if (response == null || response.markdown() == null) {
                    throw new RuntimeException("Worker returned an empty response for " + objectKey);
                }
                return response.markdown();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("Conversion attempt {}/{} for {} failed: {}", attempt, maxAttempts, objectKey, e.getMessage());
                if (attempt < maxAttempts) {
                    sleep(backoffMillis);
                    backoffMillis *= 2;
                }
            }
        }
        throw new RuntimeException("Markdown conversion failed for " + objectKey + " after "
                + maxAttempts + " attempt(s)", lastError);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry Markdown conversion", e);
        }
    }
}
