package com.abittech.platform.security;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

/**
 * Ownership: a profile is always resolved from the authenticated identity, so one user cannot read
 * or modify another user's profile - even when the request carries the other user's ids.
 */
class ProfileOwnershipTest extends IntegrationTestBase {

    @Test
    void studentACannotModifyStudentB() throws Exception {
        Account studentA = createAccount("STUDENT");
        Account studentB = createAccount("STUDENT");
        send(HttpMethod.POST, "/api/v1/student/profile", studentA.token(),
                Map.of("university", "Uni A " + UUID.randomUUID(), "programme", "Programme A"))
                .andExpect(status().isCreated());
        String bProfileId = read(send(HttpMethod.POST, "/api/v1/student/profile", studentB.token(),
                Map.of("university", "Uni B " + UUID.randomUUID(), "programme", "Programme B"))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();

        // A tries to hijack B's profile by putting B's identifiers in the body
        Map<String, Object> attack = new HashMap<>();
        attack.put("id", bProfileId);
        attack.put("userId", studentB.id().toString());
        attack.put("university", "Hacked University");
        attack.put("programme", "Hacked Programme");
        send(HttpMethod.PUT, "/api/v1/student/profile", studentA.token(), attack)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.programme").value("Hacked Programme"))
                .andExpect(jsonPath("$.data.id").value(org.hamcrest.Matchers.not(bProfileId)));

        // B is untouched
        send(HttpMethod.GET, "/api/v1/student/profile", studentB.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bProfileId))
                .andExpect(jsonPath("$.data.programme").value("Programme B"));
    }

    @Test
    void customerACannotModifyCustomerB() throws Exception {
        Account customerA = createAccount("CUSTOMER");
        Account customerB = createAccount("CUSTOMER");
        send(HttpMethod.POST, "/api/v1/customer/profile", customerA.token(),
                Map.of("customerType", "BUSINESS", "organizationName", "Company A"))
                .andExpect(status().isCreated());
        String bProfileId = read(send(HttpMethod.POST, "/api/v1/customer/profile", customerB.token(),
                Map.of("customerType", "BUSINESS", "organizationName", "Company B"))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();

        Map<String, Object> attack = new HashMap<>();
        attack.put("id", bProfileId);
        attack.put("userId", customerB.id().toString());
        attack.put("customerType", "BUSINESS");
        attack.put("organizationName", "Hacked Company");
        send(HttpMethod.PUT, "/api/v1/customer/profile", customerA.token(), attack)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationName").value("Hacked Company"))
                .andExpect(jsonPath("$.data.id").value(org.hamcrest.Matchers.not(bProfileId)));

        send(HttpMethod.GET, "/api/v1/customer/profile", customerB.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bProfileId))
                .andExpect(jsonPath("$.data.organizationName").value("Company B"));
    }

    @Test
    void eachUserOnlySeesTheirOwnProfile() throws Exception {
        Account studentA = createAccount("STUDENT");
        Account studentB = createAccount("STUDENT");
        send(HttpMethod.POST, "/api/v1/student/profile", studentB.token(),
                Map.of("university", "Uni " + UUID.randomUUID(), "programme", "B only"))
                .andExpect(status().isCreated());

        // A has no profile, and B's existence is not visible to A
        send(HttpMethod.GET, "/api/v1/student/profile", studentA.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROFILE_NOT_FOUND"));
    }

    @Test
    void thereAreNoEndpointsThatTakeAUserIdInThePath() throws Exception {
        Account student = createAccount("STUDENT");
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.PUT, "/api/v1/student/profile/" + student.id(), student.token(),
                Map.of("university", "U", "programme", "P"))
                .andExpect(status().is4xxClientError());   // no such route (404 or 405), never a success
        send(HttpMethod.GET, "/api/v1/customer/profile/" + customer.id(), customer.token(), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotReachProfilesOfTheOtherRole() throws Exception {
        Account student = createAccount("STUDENT");
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.GET, "/api/v1/customer/profile", student.token(), null).andExpect(status().isForbidden());
        send(HttpMethod.GET, "/api/v1/student/profile", customer.token(), null).andExpect(status().isForbidden());
    }
}
