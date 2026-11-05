package com.abittech.platform.servicerequest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class ServiceRequestApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/customer/service-requests";

    private Map<String, Object> request(String slug) {
        Map<String, Object> body = new HashMap<>();
        body.put("serviceSlug", slug);
        body.put("title", "Company website");
        body.put("description", "We need a modern website with a contact form and a service catalogue.");
        return body;
    }

    private String create(Account customer, String slug) throws Exception {
        return read(send(HttpMethod.POST, URL, customer.token(), request(slug))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();
    }

    private void adminSetStatus(String admin, String id, String status, int expectedHttp) throws Exception {
        send(HttpMethod.PUT, "/api/v1/admin/service-requests/" + id + "/status", admin, Map.of("status", status))
                .andExpect(status().is(expectedHttp));
    }

    // ---------- customers ----------

    @Test
    void customerCanCreateListAndReadOwnRequests() throws Exception {
        Account customer = createCustomerWithProfile();

        send(HttpMethod.POST, URL, customer.token(), request("web-development"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.serviceSlug").value("web-development"))
                .andExpect(jsonPath("$.data.serviceName").value("Web Development"))
                .andExpect(jsonPath("$.data.title").value("Company website"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        send(HttpMethod.GET, URL, customer.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void customerCanHaveSeveralRequests() throws Exception {
        Account customer = createCustomerWithProfile();
        String first = create(customer, "web-development");
        create(customer, "cybersecurity");

        send(HttpMethod.GET, URL, customer.token(), null)
                .andExpect(jsonPath("$.data.length()").value(2));
        send(HttpMethod.GET, URL + "/" + first, customer.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(first));
    }

    @Test
    void requestNeedsAKnownActiveService() throws Exception {
        Account customer = createCustomerWithProfile();

        send(HttpMethod.POST, URL, customer.token(), request("time-travel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void requestIsValidated() throws Exception {
        Account customer = createCustomerWithProfile();
        Map<String, Object> body = new HashMap<>();
        body.put("serviceSlug", "");
        body.put("title", "");
        body.put("description", "short");

        send(HttpMethod.POST, URL, customer.token(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'serviceSlug')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'description')]").exists());
    }

    @Test
    void customerWithoutProfileMustCreateOneFirst() throws Exception {
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.POST, URL, customer.token(), request("web-development"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PROFILE_NOT_FOUND"));
    }

    @Test
    void customersCannotSeeOrCancelEachOthersRequests() throws Exception {
        Account customerA = createCustomerWithProfile();
        Account customerB = createCustomerWithProfile();
        String bRequest = create(customerB, "data-analysis");

        send(HttpMethod.GET, URL, customerA.token(), null)
                .andExpect(jsonPath("$.data.length()").value(0));
        send(HttpMethod.GET, URL + "/" + bRequest, customerA.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_REQUEST_NOT_FOUND"));
        send(HttpMethod.POST, URL + "/" + bRequest + "/cancel", customerA.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_REQUEST_NOT_FOUND"));

        send(HttpMethod.GET, URL + "/" + bRequest, customerB.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    void customerCanCancelASubmittedRequestOnlyOnce() throws Exception {
        Account customer = createCustomerWithProfile();
        String id = create(customer, "web-development");

        send(HttpMethod.POST, URL + "/" + id + "/cancel", customer.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        send(HttpMethod.POST, URL + "/" + id + "/cancel", customer.token(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void endpointsRequireTheCustomerRoleAndAuthentication() throws Exception {
        Account student = createAccount("STUDENT");

        send(HttpMethod.POST, URL, student.token(), request("web-development"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        send(HttpMethod.GET, URL, student.token(), null).andExpect(status().isForbidden());
        send(HttpMethod.GET, URL, null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ---------- staff ----------

    @Test
    void adminCanSeeWhoRequestedWhatAndFilterByStatus() throws Exception {
        String admin = createAdminToken();
        Account customer = createCustomerWithProfile();
        String id = create(customer, "mobile-app-development");

        send(HttpMethod.GET, "/api/v1/admin/service-requests?status=SUBMITTED&size=100", admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + id + "')].customerEmail").value(customer.email()));
        send(HttpMethod.GET, "/api/v1/admin/service-requests/" + id, admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerFullName").value("Test User"))
                .andExpect(jsonPath("$.data.serviceName").value("Mobile App Development"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    void adminCanMoveARequestThroughTheWholeLifecycle() throws Exception {
        String admin = createAdminToken();
        Account customer = createCustomerWithProfile();
        String id = create(customer, "software-development");

        adminSetStatus(admin, id, "IN_REVIEW", 200);
        adminSetStatus(admin, id, "ACCEPTED", 200);
        adminSetStatus(admin, id, "IN_PROGRESS", 200);
        adminSetStatus(admin, id, "COMPLETED", 200);

        send(HttpMethod.GET, URL + "/" + id, customer.token(), null)
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void invalidTransitionsAreRejected() throws Exception {
        String admin = createAdminToken();
        Account customer = createCustomerWithProfile();
        String id = create(customer, "software-development");

        adminSetStatus(admin, id, "COMPLETED", 409);   // cannot skip steps
        adminSetStatus(admin, id, "SUBMITTED", 409);   // not a change
        send(HttpMethod.PUT, "/api/v1/admin/service-requests/" + id + "/status", admin, Map.of("status", "COMPLETED"))
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        adminSetStatus(admin, id, "REJECTED", 200);
        adminSetStatus(admin, id, "IN_REVIEW", 409);   // final status
    }

    @Test
    void customerCannotCancelOnceWorkIsAccepted() throws Exception {
        String admin = createAdminToken();
        Account customer = createCustomerWithProfile();
        String id = create(customer, "web-development");
        adminSetStatus(admin, id, "IN_REVIEW", 200);

        send(HttpMethod.POST, URL + "/" + id + "/cancel", customer.token(), null).andExpect(status().isOk());

        String second = create(customer, "web-development");
        adminSetStatus(admin, second, "IN_REVIEW", 200);
        adminSetStatus(admin, second, "ACCEPTED", 200);
        send(HttpMethod.POST, URL + "/" + second + "/cancel", customer.token(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void adminServiceRequestEndpointsAreForAdminsOnly() throws Exception {
        Account customer = createCustomerWithProfile();
        String id = create(customer, "web-development");

        send(HttpMethod.GET, "/api/v1/admin/service-requests", customer.token(), null)
                .andExpect(status().isForbidden());
        send(HttpMethod.PUT, "/api/v1/admin/service-requests/" + id + "/status", customer.token(),
                Map.of("status", "COMPLETED")).andExpect(status().isForbidden());
        send(HttpMethod.GET, "/api/v1/admin/service-requests", null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRequestReturnsNotFoundForAdmin() throws Exception {
        send(HttpMethod.GET, "/api/v1/admin/service-requests/" + UUID.randomUUID(), createAdminToken(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_REQUEST_NOT_FOUND"));
    }
}
