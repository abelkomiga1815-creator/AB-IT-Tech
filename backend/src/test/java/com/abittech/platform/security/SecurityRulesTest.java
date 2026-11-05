package com.abittech.platform.security;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.config.JwtProperties;
import com.abittech.platform.support.IntegrationTestBase;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

/**
 * URL-level security rules: who may reach what, and the JSON format of 401/403.
 */
class SecurityRulesTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private JwtService jwtService;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/me", "/api/v1/profile", "/api/v1/customer/profile", "/api/v1/student/profile"})
    void protectedEndpointsReturn401JsonWithoutToken(String url) throws Exception {
        send(HttpMethod.GET, url, null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        send(HttpMethod.GET, "/api/v1/auth/me", "not-a-jwt", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenSignedWithAnotherSecretReturns401() throws Exception {
        Account account = createAccount("STUDENT");
        JwtService foreign = new JwtService(
                new JwtProperties("foreign-secret-0123456789-abcdefghijklmnopqrstuvwxyz", 3_600_000), Clock.systemUTC());
        String token = foreign.generateToken(account.id(), account.email(), Role.STUDENT);

        send(HttpMethod.GET, "/api/v1/auth/me", token, null).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        Account account = createAccount("STUDENT");
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), java.time.ZoneOffset.UTC);
        String expired = new JwtService(jwtProperties, past).generateToken(account.id(), account.email(), Role.STUDENT);

        send(HttpMethod.GET, "/api/v1/auth/me", expired, null).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfAnUnknownUserReturns401() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), "ghost@example.com", Role.STUDENT);

        send(HttpMethod.GET, "/api/v1/auth/me", token, null).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfADisabledAccountIsRejectedImmediately() throws Exception {
        Account account = createAccount("STUDENT");
        send(HttpMethod.GET, "/api/v1/auth/me", account.token(), null).andExpect(status().isOk());

        User user = userRepository.findById(account.id()).orElseThrow();
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        send(HttpMethod.GET, "/api/v1/auth/me", account.token(), null).andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotUseCustomerEndpoints() throws Exception {
        Account student = createAccount("STUDENT");
        for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT}) {
            send(method, "/api/v1/customer/profile", student.token(), Map.of("customerType", "INDIVIDUAL"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Test
    void customerCannotUseStudentEndpoints() throws Exception {
        Account customer = createAccount("CUSTOMER");
        for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT}) {
            send(method, "/api/v1/student/profile", customer.token(),
                    Map.of("university", "U", "programme", "P"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Test
    void adminHasNoCustomerOrStudentProfileButCanUseGenericEndpoints() throws Exception {
        User admin = userRepository.saveAndFlush(
                new User("Admin", "admin-" + UUID.randomUUID() + "@example.com", "hash", Role.ADMIN));
        String token = jwtService.generateToken(admin.getId(), admin.getEmail(), Role.ADMIN);

        send(HttpMethod.GET, "/api/v1/auth/me", token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
        send(HttpMethod.GET, "/api/v1/profile", token, null).andExpect(status().isOk());
        send(HttpMethod.GET, "/api/v1/customer/profile", token, null).andExpect(status().isForbidden());
        send(HttpMethod.GET, "/api/v1/student/profile", token, null).andExpect(status().isForbidden());
    }

    @Test
    void publicEndpointsDoNotRequireAToken() throws Exception {
        send(HttpMethod.GET, "/api/v1/health", null, null).andExpect(status().isOk());
        // reachable (not 401): an empty body is rejected by validation, not by security
        send(HttpMethod.POST, "/api/v1/auth/login", null, Map.of()).andExpect(status().isBadRequest());
        send(HttpMethod.POST, "/api/v1/auth/register", null, Map.of()).andExpect(status().isBadRequest());
    }

    @Test
    void corsPreflightFromAnAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void corsPreflightFromAnotherOriginIsRejected() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/v1/auth/login")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
