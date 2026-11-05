package com.abittech.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Registration, login and /auth/me.
 */
class AuthApiTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    private Map<String, Object> registration(String email, String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "John Doe");
        body.put("email", email);
        body.put("password", PASSWORD);
        if (role != null) {
            body.put("role", role);
        }
        return body;
    }

    private String uniqueEmail() {
        return "auth-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void registerCreatesAccountWithSafeResponseAndDefaultsToCustomer() throws Exception {
        String email = uniqueEmail();

        send(HttpMethod.POST, "/api/v1/auth/register", null, registration(email, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.fullName").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("$2"))));
    }

    @Test
    void registerAcceptsStudentRole() throws Exception {
        send(HttpMethod.POST, "/api/v1/auth/register", null, registration(uniqueEmail(), "STUDENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void registerCannotCreateAnAdmin() throws Exception {
        send(HttpMethod.POST, "/api/v1/auth/register", null, registration(uniqueEmail(), "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROLE"));
    }

    @Test
    void registerStoresNormalisedEmailAndBcryptHash() throws Exception {
        String email = uniqueEmail();

        send(HttpMethod.POST, "/api/v1/auth/register", null, registration(email.toUpperCase(), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email));

        User stored = userRepository.findByEmail(email).orElseThrow();
        assertThat(stored.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        String email = uniqueEmail();
        send(HttpMethod.POST, "/api/v1/auth/register", null, registration(email, null))
                .andExpect(status().isCreated());

        send(HttpMethod.POST, "/api/v1/auth/register", null, registration(email.toUpperCase(), null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void registerValidatesInput() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "");
        body.put("email", "not-an-email");
        body.put("password", "short");

        send(HttpMethod.POST, "/api/v1/auth/register", null, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    void loginReturnsUsableJwtAndNoPassword() throws Exception {
        Account account = createAccount("STUDENT");

        MvcResult login = send(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("email", account.email(), "password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.email").value(account.email()))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn();

        String token = read(login).at("/data/accessToken").asText();
        send(HttpMethod.GET, "/api/v1/auth/me", token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(account.id().toString()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void loginIsCaseInsensitiveForEmail() throws Exception {
        Account account = createAccount(null);

        send(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("email", account.email().toUpperCase(), "password", PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameGenericError() throws Exception {
        Account account = createAccount(null);

        MvcResult wrongPassword = send(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("email", account.email(), "password", "Wrong-Passw0rd"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andReturn();
        MvcResult unknownEmail = send(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("email", uniqueEmail(), "password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        assertThat(read(wrongPassword).get("message")).isEqualTo(read(unknownEmail).get("message"));
    }

    @Test
    void disabledAccountCannotLogInAndGetsTheSameGenericError() throws Exception {
        Account account = createAccount(null);
        User user = userRepository.findById(account.id()).orElseThrow();
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        send(HttpMethod.POST, "/api/v1/auth/login", null, Map.of("email", account.email(), "password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        send(HttpMethod.GET, "/api/v1/auth/me", null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
