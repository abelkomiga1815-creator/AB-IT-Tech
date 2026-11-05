package com.abittech.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The client is tested against a mock server: no network, no AI service needed.
 */
class AiClientTest {

    private static final String KEY = "unit-test-ai-key-0123456789-abcdefghijklmnopqrstuvwxyz";
    private static final String URL = "http://ai.test/api/v1/research/report";

    private MockRestServiceServer server;
    private AiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiClient(new AiProperties("http://ai.test", KEY, 1000, 1000, 60), builder);
    }

    private static ApiException thrownCode(Runnable call, ErrorCode expected) {
        ApiException failure = null;
        try {
            call.run();
        } catch (ApiException ex) {
            failure = ex;
        }
        assertThat(failure).isNotNull();
        assertThat(failure.getErrorCode()).isEqualTo(expected);
        return failure;
    }

    @Test
    void sendsTheKeyAndTheProjectTextAndReturnsTheReportData() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", KEY))
                .andExpect(jsonPath("$.title").value("Crop disease detection"))
                .andExpect(jsonPath("$.abstract").value("Leaf images."))
                .andExpect(jsonPath("$.maxResults").value(10))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"retrieved\":{\"papersFound\":3}}}",
                        MediaType.APPLICATION_JSON));

        JsonNode data = client.analyze("Crop disease detection", "Leaf images.", 10);

        assertThat(data.at("/retrieved/papersFound").asInt()).isEqualTo(3);
        server.verify();
    }

    @Test
    void omitsTheAbstractWhenThereIsNone() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.title").value("A title"))
                .andExpect(jsonPath("$.abstract").doesNotExist())
                .andRespond(withSuccess("{\"success\":true,\"data\":{}}", MediaType.APPLICATION_JSON));

        client.analyze("A title", "  ", 5);
        server.verify();
    }

    @Test
    void unreachableServiceIsReportedAsUnavailable() {
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new IOException("connection refused");
        });

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void aWrongSharedKeyIsAServiceErrorNotAStudentError() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":false,\"code\":\"UNAUTHORIZED\"}"));

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    void serverErrorsAreServiceErrors() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    void serviceUnavailableAnswerIsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void unexpectedBodiesAreRejected() {
        for (String body : new String[] {"{}", "{\"success\":false}", "{\"success\":true}", "{\"success\":true,\"data\":null}"}) {
            server.reset();
            server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Test
    void theErrorShownToCallersNeverContainsTheKey() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.analyze("A title", null, 5))
                .hasMessageNotContaining(KEY);
    }
}
