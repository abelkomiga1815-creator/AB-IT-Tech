package com.abittech.platform.customer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class CustomerProfileApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/customer/profile";

    private Map<String, Object> businessProfile() {
        Map<String, Object> body = new HashMap<>();
        body.put("customerType", "BUSINESS");
        body.put("organizationName", "Acme Ltd");
        body.put("phone", "+255 712 345 678");
        body.put("address", "12 Main Street");
        body.put("city", "Dar es Salaam");
        body.put("country", "Tanzania");
        return body;
    }

    @Test
    void customerCanCreateProfile() throws Exception {
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.POST, URL, customer.token(), businessProfile())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.customerType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.organizationName").value("Acme Ltd"))
                .andExpect(jsonPath("$.data.phone").value("+255 712 345 678"))
                .andExpect(jsonPath("$.data.city").value("Dar es Salaam"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void customerCanRetrieveProfile() throws Exception {
        Account customer = createAccount("CUSTOMER");
        send(HttpMethod.POST, URL, customer.token(), businessProfile()).andExpect(status().isCreated());

        send(HttpMethod.GET, URL, customer.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.organizationName").value("Acme Ltd"));
    }

    @Test
    void customerCanUpdateProfile() throws Exception {
        Account customer = createAccount("CUSTOMER");
        send(HttpMethod.POST, URL, customer.token(), businessProfile()).andExpect(status().isCreated());

        Map<String, Object> update = businessProfile();
        update.put("organizationName", "Acme Holdings");
        update.put("city", "Arusha");
        update.put("address", "  ");

        send(HttpMethod.PUT, URL, customer.token(), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationName").value("Acme Holdings"))
                .andExpect(jsonPath("$.data.city").value("Arusha"))
                .andExpect(jsonPath("$.data.address").doesNotExist());

        send(HttpMethod.GET, URL, customer.token(), null)
                .andExpect(jsonPath("$.data.organizationName").value("Acme Holdings"));
    }

    @Test
    void duplicateProfileIsRejected() throws Exception {
        Account customer = createAccount("CUSTOMER");
        send(HttpMethod.POST, URL, customer.token(), businessProfile()).andExpect(status().isCreated());

        send(HttpMethod.POST, URL, customer.token(), businessProfile())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CUSTOMER_PROFILE_ALREADY_EXISTS"));
    }

    @Test
    void getAndUpdateBeforeCreationReturnNotFound() throws Exception {
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.GET, URL, customer.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PROFILE_NOT_FOUND"));
        send(HttpMethod.PUT, URL, customer.token(), businessProfile())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PROFILE_NOT_FOUND"));
    }

    @Test
    void nonCustomerCannotCreateCustomerProfile() throws Exception {
        Account student = createAccount("STUDENT");

        send(HttpMethod.POST, URL, student.token(), businessProfile())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        send(HttpMethod.POST, URL, null, businessProfile())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        send(HttpMethod.GET, URL, null, null).andExpect(status().isUnauthorized());
        send(HttpMethod.PUT, URL, null, businessProfile()).andExpect(status().isUnauthorized());
    }

    @Test
    void individualDoesNotNeedAnOrganizationName() throws Exception {
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.POST, URL, customer.token(), Map.of("customerType", "INDIVIDUAL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.customerType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.data.organizationName").doesNotExist());
    }

    @Test
    void organisationTypesRequireAnOrganizationName() throws Exception {
        Account customer = createAccount("CUSTOMER");
        for (String type : new String[] {"BUSINESS", "ORGANIZATION", "INSTITUTION", "STARTUP"}) {
            send(HttpMethod.POST, URL, customer.token(), Map.of("customerType", type))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PROFILE_DATA"));
        }
        // OTHER behaves like INDIVIDUAL
        send(HttpMethod.POST, URL, customer.token(), Map.of("customerType", "OTHER"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateAlsoEnforcesTheOrganizationRule() throws Exception {
        Account customer = createAccount("CUSTOMER");
        send(HttpMethod.POST, URL, customer.token(), Map.of("customerType", "INDIVIDUAL"))
                .andExpect(status().isCreated());

        send(HttpMethod.PUT, URL, customer.token(), Map.of("customerType", "STARTUP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROFILE_DATA"));
    }

    @Test
    void invalidInputIsRejectedWithFieldErrors() throws Exception {
        Account customer = createAccount("CUSTOMER");
        Map<String, Object> body = new HashMap<>();
        body.put("organizationName", "No Type Ltd");
        body.put("phone", "call-me-maybe");
        body.put("city", "x".repeat(101));

        send(HttpMethod.POST, URL, customer.token(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'customerType')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'phone')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'city')]").exists());
    }

    @Test
    void unknownCustomerTypeIsRejected() throws Exception {
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.POST, URL, customer.token(), Map.of("customerType", "WIZARD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
