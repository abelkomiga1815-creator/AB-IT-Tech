package com.abittech.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The client is tested against a real local HTTP server: no network, no AI service needed.
 */
class AiClientTest {

    private static final String KEY = "unit-test-ai-key-0123456789-abcdefghijklmnopqrstuvwxyz";

    private HttpServer server;
    private int port;
    private AiClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/v1/research/report", new ReportHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        AiProperties properties = new AiProperties("http://127.0.0.1:" + port, KEY, 1000, 1000, 60);
        client = new AiClient(properties);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
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
    void sendsTheKeyAndTheProjectTextAndReturnsTheReportData() throws Exception {
        setResponse(200, "{\"success\":true,\"data\":{\"retrieved\":{\"papersFound\":3}}}");

        JsonNode data = client.analyze("Crop disease detection", "Leaf images.", 10);

        assertThat(data.at("/retrieved/papersFound").asInt()).isEqualTo(3);
    }

    @Test
    void omitsTheAbstractWhenThereIsNone() throws Exception {
        setResponse(200, "{\"success\":true,\"data\":{}}");

        client.analyze("A title", "  ", 5);
    }

    @Test
    void unreachableServiceIsReportedAsUnavailable() throws Exception {
        server.stop(0);
        server = null;

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void aWrongSharedKeyIsAServiceErrorNotAStudentError() throws Exception {
        setResponse(401, "{\"success\":false,\"code\":\"UNAUTHORIZED\"}");

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    void serverErrorsAreServiceErrors() throws Exception {
        setResponse(500, "");

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    void serviceUnavailableAnswerIsUnavailable() throws Exception {
        setResponse(503, "");

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void unexpectedBodiesAreRejected() throws Exception {
        for (String body : new String[] {"{}", "{\"success\":false}", "{\"success\":true}", "{\"success\":true,\"data\":null}"}) {
            setResponse(200, body);
            thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Test
    void validationErrorFromAiServiceIsReportedAsValidationError() throws Exception {
        setResponse(400, "{\"success\":false,\"code\":\"VALIDATION_ERROR\",\"message\":\"Request validation failed\","
                + "\"errors\":[{\"field\":\"title\",\"message\":\"String should have at least 3 characters\"}]}");

        ApiException failure = thrownCode(() -> client.analyze("ab", null, 5), ErrorCode.VALIDATION_ERROR);
        assertThat(failure.getMessage()).contains("title");
        assertThat(failure.getMessage()).contains("3 characters");
    }

    @Test
    void plainFourHundredWithoutValidationBodyRemainsAServiceError() throws Exception {
        setResponse(400, "{\"success\":false,\"code\":\"BAD_REQUEST\",\"message\":\"The request is invalid\"}");

        thrownCode(() -> client.analyze("A title", null, 5), ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    void theErrorShownToCallersNeverContainsTheKey() throws Exception {
        setResponse(500, "");

        assertThatThrownBy(() -> client.analyze("A title", null, 5))
                .hasMessageNotContaining(KEY);
    }

    // ---------- test server ----------

    private volatile String lastRequestBody = "";
    private volatile String nextResponseBody = "";
    private volatile int nextResponseStatus = 200;

    private void setResponse(int status, String body) {
        nextResponseStatus = status;
        nextResponseBody = body;
    }

    private class ReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Read request body
            int len = exchange.getRequestBody().available();
            byte[] input = new byte[len > 0 ? len : 1024];
            int read = exchange.getRequestBody().read(input);
            lastRequestBody = new String(input, 0, read, StandardCharsets.UTF_8);

            // Check API key
            String key = exchange.getRequestHeaders().getFirst("X-API-Key");
            if (!KEY.equals(key)) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                String body = "{\"success\":false,\"code\":\"UNAUTHORIZED\"}";
                exchange.sendResponseHeaders(401, body.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            // Send configured response
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bodyBytes = nextResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(nextResponseStatus, bodyBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bodyBytes);
            }
        }
    }
}
