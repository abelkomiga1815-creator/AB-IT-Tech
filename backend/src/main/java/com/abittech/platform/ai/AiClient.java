package com.abittech.platform.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.config.AiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

/**
 * Client of the Python AI research engine (GROUP 4). Only the backend calls it; browsers never do.
 * The shared secret goes in the {@code X-API-Key} header and is never logged.
 * <p>
 * Uses {@link java.net.http.HttpClient} directly so the JSON request body and the response body are
 * transmitted reliably, without depending on Spring's {@code RestClient} request-factory behaviour.
 */
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    static final String REPORT_PATH = "/api/v1/research/report";

    private static String redactSensitive(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        result = result.replaceAll("[sS]{32,}", "[REDACTED-LONG-SEQ]");
        if (result.length() > 200) {
            result = result.substring(0, 200) + " [...] (truncated)";
        }
        return result;
    }

    private final HttpClient httpClient;
    private final URI baseUri;
    private final String apiKey;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiClient(AiProperties properties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();
        this.baseUri = URI.create(properties.baseUrl());
        this.apiKey = properties.apiKey();
        this.connectTimeoutMs = properties.connectTimeoutMs();
        this.readTimeoutMs = properties.readTimeoutMs();
    }

    /**
     * Runs a full research analysis.
     *
     * @return the {@code data} object of the AI service's report
     * @throws ApiException {@code AI_SERVICE_UNAVAILABLE} when the service cannot be reached or times out,
     *                      {@code AI_SERVICE_ERROR} for any other problem (wrong key, bad answer, server error)
     */
    public JsonNode analyze(String title, String abstractText, int maxResults) {
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(bodyMap(title, abstractText, maxResults));
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "Failed to serialize research request");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUri.resolve(REPORT_PATH))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofMillis(readTimeoutMs))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("X-API-Key", apiKey)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("User-Agent", "AB-IT-TECH-ResearchClient/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String responseBody = response.body();

            if (status >= 200 && status < 300) {
                JsonNode tree = objectMapper.readTree(responseBody);
                if (tree == null || !tree.path("success").asBoolean(false) || !tree.hasNonNull("data")) {
                    log.error("AI service returned an unexpected body");
                    throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
                }
                return tree.get("data");
            }

            // Redact sensitive fields from the response body before logging
            String redacted = redactSensitive(responseBody);
            log.error("AI service answered with HTTP {} body={}", status, redacted);

            // A 400 VALIDATION_ERROR from the AI service describes exactly which request field is
            // invalid. Surface it as a client validation error (with the field and reason) instead of
            // collapsing it into a generic service failure.
            Optional<ApiException> validationFailure = validationFailure(responseBody);
            if (validationFailure.isPresent()) {
                throw validationFailure.get();
            }

            if (status == 503) {
                throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
        } catch (java.net.http.HttpTimeoutException ex) {
            log.warn("AI service timed out");
            throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (java.io.IOException ex) {
            // Connection refused, network unreachable, etc.
            log.warn("AI service not reachable ({})", ex.getClass().getSimpleName());
            throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (Exception ex) {
            if (ex instanceof ApiException) {
                throw (ApiException) ex;
            }
            log.error("AI service call failed ({})", ex.getClass().getSimpleName());
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    /**
     * Parses a FastAPI {@code VALIDATION_ERROR} response body into a client-facing {@link ApiException}
     * that names the invalid field and the reason. Returns empty when the body is not a validation error
     * or cannot be parsed, so the caller keeps its generic status-based handling.
     * <p>
     * Only field names and generic validator messages are used; no project content or secrets are
     * included in the returned exception message.
     */
    private Optional<ApiException> validationFailure(String responseBody) {
        try {
            JsonNode errorTree = objectMapper.readTree(responseBody);
            if (!"VALIDATION_ERROR".equals(errorTree.path("code").asText())) {
                return Optional.empty();
            }
            JsonNode errors = errorTree.path("errors");
            String field = "unknown";
            String reason = errorTree.path("message").asText("Request validation failed");
            if (errors.isArray() && errors.size() > 0) {
                field = errors.get(0).path("field").asText("unknown");
                reason = errors.get(0).path("message").asText(reason);
            }
            return Optional.of(new ApiException(ErrorCode.VALIDATION_ERROR, field + ": " + reason));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Builds the request body map for the AI service. The abstract is omitted when blank so the
     * FastAPI model treats it as optional.
     */
    private static Map<String, Object> bodyMap(String title, String abstractText, int maxResults) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        if (abstractText != null && !abstractText.isBlank()) {
            body.put("abstract", abstractText);
        }
        body.put("maxResults", maxResults);
        return body;
    }
}
