package com.abittech.platform.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void successResponseWrapsData() throws Exception {
        String json = mapper.writeValueAsString(ApiResponse.ok(Map.of("name", "AB IT TECH")));
        assertThat(mapper.readTree(json).get("success").asBoolean()).isTrue();
        assertThat(mapper.readTree(json).get("data").get("name").asText()).isEqualTo("AB IT TECH");
    }

    @Test
    void emptySuccessResponseOmitsData() throws Exception {
        String json = mapper.writeValueAsString(ApiResponse.ok());
        assertThat(mapper.readTree(json).get("success").asBoolean()).isTrue();
        assertThat(mapper.readTree(json).has("data")).isFalse();
    }
}
