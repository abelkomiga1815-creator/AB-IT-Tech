package com.abittech.platform.ai;

import java.util.HashMap;
import java.util.Map;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client of the Python AI research engine (GROUP 4). Only the backend calls it; browsers never do.
 * The shared secret goes in the {@code X-API-Key} header and is never logged.
 */
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    static final String REPORT_PATH = "/api/v1/research/report";

    private static String redactSensitive(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // Redact common sensitive patterns without regex that could fail on binary data
        String result = input;
        // Do not log API keys, tokens, passwords, or authorization headers
        result = result.replaceAll("[sS]{32,}", "[REDACTED-LONG-SEQ]");
        // Do not log full abstracts or project content (keep first 50 chars if too long)
        if (result.length() > 200) {
            result = result.substring(0, 200) + " [...] (truncated)";
        }
        return result;
    }

    private final RestClient restClient;

    public AiClient(AiProperties properties, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-API-Key", properties.apiKey())
                .build();
    }

    /**
     * Runs a full research analysis.
     *
     * @return the {@code data} object of the AI service's report
     * @throws ApiException {@code AI_SERVICE_UNAVAILABLE} when the service cannot be reached or times out,
     *                      {@code AI_SERVICE_ERROR} for any other problem (wrong key, bad answer, server error)
     */
    public JsonNode analyze(String title, String abstractText, int maxResults) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        if (abstractText != null && !abstractText.isBlank()) {
            body.put("abstract", abstractText);
        }
        body.put("maxResults", maxResults);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri(REPORT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException ex) {
            log.warn("AI service not reachable ({})", ex.getClass().getSimpleName());
            throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            String responseBody = "no body";
            try {
                responseBody = ex.getResponseBodyAsString();
            } catch (Exception ignored) {
                // ignore unable to read body
            }
            // Redact sensitive fields from the response body before logging
            String redacted = redactSensitive(responseBody);
            log.error("AI service answered with HTTP {} body={}", status, redacted);
            // Distinguish 503 (unavailable) from other errors (4xx validation, 5xx server error)
            if (status == 503) {
                throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            // 4xx client errors: treat as request rejection, not service outage
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
        } catch (RestClientException ex) {
            log.error("AI service call failed ({})", ex.getClass().getSimpleName());
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
        }

        if (response == null || !response.path("success").asBoolean(false) || !response.hasNonNull("data")) {
            log.error("AI service returned an unexpected body");
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR);
        }
        return response.get("data");
    }
}
