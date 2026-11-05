package com.abittech.platform.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.security.JwtService;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base class for API tests: full Spring context on an in-memory H2 database, real security filter
 * chain, real Flyway migrations. Accounts are created through the public API (register + login),
 * with unique e-mail addresses, so tests do not depend on each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Str0ng-Passw0rd";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JwtService jwtService;

    /** A registered and logged-in account. */
    public record Account(UUID id, String email, String token) {
    }

    /** Registers (role may be null for the default) and logs in a brand-new account. */
    protected Account createAccount(String role) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Test User");
        body.put("email", email);
        body.put("password", PASSWORD);
        if (role != null) {
            body.put("role", role);
        }
        MvcResult registered = send(HttpMethod.POST, "/api/v1/auth/register", null, body)
                .andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(read(registered).at("/data/id").asText());
        MvcResult loggedIn = send(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("email", email, "password", PASSWORD))
                .andExpect(status().isOk()).andReturn();
        return new Account(id, email, read(loggedIn).at("/data/accessToken").asText());
    }

    /** Creates an ADMIN directly in the database (there is no admin registration) and returns its token. */
    protected String createAdminToken() {
        User admin = userRepository.saveAndFlush(
                new User("Admin", "admin-" + UUID.randomUUID() + "@example.com", "hash", Role.ADMIN));
        return jwtService.generateToken(admin.getId(), admin.getEmail(), Role.ADMIN);
    }

    /** A CUSTOMER account that has already created its (individual) customer profile. */
    protected Account createCustomerWithProfile() throws Exception {
        Account customer = createAccount("CUSTOMER");
        send(HttpMethod.POST, "/api/v1/customer/profile", customer.token(),
                Map.of("customerType", "INDIVIDUAL", "phone", "+255 712 000 111"))
                .andExpect(status().isCreated());
        return customer;
    }

    /** A STUDENT account that has already created its student profile. */
    protected Account createStudentWithProfile() throws Exception {
        Account student = createAccount("STUDENT");
        send(HttpMethod.POST, "/api/v1/student/profile", student.token(),
                Map.of("university", "University " + UUID.randomUUID(), "programme", "Computer Science"))
                .andExpect(status().isCreated());
        return student;
    }

    /** Creates a project for the student and returns its id. */
    protected String createProject(Account student, String title) throws Exception {
        return read(send(HttpMethod.POST, "/api/v1/student/projects", student.token(), Map.of("title", title))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();
    }

    protected ResultActions send(HttpMethod method, String url, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, url);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
