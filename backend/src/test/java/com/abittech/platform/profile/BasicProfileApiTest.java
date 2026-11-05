package com.abittech.platform.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import com.abittech.platform.support.IntegrationTestBase;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

class BasicProfileApiTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    void getReturnsBasicAccountInformationWithoutPassword() throws Exception {
        Account account = createAccount("STUDENT");

        send(HttpMethod.GET, "/api/v1/profile", account.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(account.id().toString()))
                .andExpect(jsonPath("$.data.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.email").value(account.email()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void putUpdatesFullName() throws Exception {
        Account account = createAccount(null);

        send(HttpMethod.PUT, "/api/v1/profile", account.token(), Map.of("fullName", "  Jane Smith "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Jane Smith"));

        send(HttpMethod.GET, "/api/v1/profile", account.token(), null)
                .andExpect(jsonPath("$.data.fullName").value("Jane Smith"));
    }

    @Test
    void putCannotChangeEmailRoleEnabledOrPassword() throws Exception {
        Account account = createAccount("STUDENT");
        String originalHash = userRepository.findById(account.id()).orElseThrow().getPassword();

        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "New Name");
        body.put("email", "hacker@example.com");
        body.put("role", "ADMIN");
        body.put("enabled", false);
        body.put("password", "Hacked-Passw0rd");
        body.put("id", java.util.UUID.randomUUID().toString());

        send(HttpMethod.PUT, "/api/v1/profile", account.token(), body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("New Name"))
                .andExpect(jsonPath("$.data.email").value(account.email()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.id").value(account.id().toString()));

        User stored = userRepository.findById(account.id()).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo(account.email());
        assertThat(stored.getRole()).isEqualTo(Role.STUDENT);
        assertThat(stored.isEnabled()).isTrue();
        assertThat(stored.getPassword()).isEqualTo(originalHash);
    }

    @Test
    void putRejectsBlankFullName() throws Exception {
        Account account = createAccount(null);

        send(HttpMethod.PUT, "/api/v1/profile", account.token(), Map.of("fullName", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").exists());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        send(HttpMethod.GET, "/api/v1/profile", null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        send(HttpMethod.PUT, "/api/v1/profile", null, Map.of("fullName", "X"))
                .andExpect(status().isUnauthorized());
    }
}
