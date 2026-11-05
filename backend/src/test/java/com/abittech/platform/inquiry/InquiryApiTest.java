package com.abittech.platform.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.abittech.platform.inquiry.entity.CustomerInquiry;
import com.abittech.platform.inquiry.repository.CustomerInquiryRepository;
import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

class InquiryApiTest extends IntegrationTestBase {

    private static final String MESSAGE = "We would like a quote for a new company website.";

    @Autowired
    private CustomerInquiryRepository inquiryRepository;

    private Map<String, Object> guestInquiry(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Guest Visitor");
        body.put("email", email);
        body.put("phone", "+255 755 123 456");
        body.put("serviceSlug", "web-development");
        body.put("message", MESSAGE);
        return body;
    }

    // ---------- guests ----------

    @Test
    void guestCanSubmitAnInquiryWithoutLoggingIn() throws Exception {
        String email = "Guest-" + UUID.randomUUID() + "@Example.com";

        MvcResult result = send(HttpMethod.POST, "/api/v1/inquiries", null, guestInquiry(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("NEW"))
                // no personal data is echoed to an anonymous caller
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(content().string(not(containsString("Guest Visitor"))))
                .andReturn();

        UUID id = UUID.fromString(read(result).at("/data/id").asText());
        CustomerInquiry stored = inquiryRepository.findWithServiceById(id).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo(email.toLowerCase());
        assertThat(stored.getCustomerProfile()).isNull();
        assertThat(stored.getService().getSlug()).isEqualTo("web-development");
    }

    @Test
    void guestInquiryWithoutServiceIsAllowed() throws Exception {
        Map<String, Object> body = guestInquiry("guest-" + UUID.randomUUID() + "@example.com");
        body.remove("serviceSlug");
        body.remove("phone");

        send(HttpMethod.POST, "/api/v1/inquiries", null, body).andExpect(status().isCreated());
    }

    @Test
    void guestInquiryIsValidated() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "");
        body.put("email", "not-an-email");
        body.put("message", "short");
        body.put("phone", "abc");

        send(HttpMethod.POST, "/api/v1/inquiries", null, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'message')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'phone')]").exists());
    }

    @Test
    void guestInquiryWithUnknownServiceIsRejected() throws Exception {
        Map<String, Object> body = guestInquiry("guest-" + UUID.randomUUID() + "@example.com");
        body.put("serviceSlug", "time-travel");

        send(HttpMethod.POST, "/api/v1/inquiries", null, body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void guestEndpointIgnoresACustomerTokenAndStaysAnonymous() throws Exception {
        Account customer = createCustomerWithProfile();

        MvcResult result = send(HttpMethod.POST, "/api/v1/inquiries", customer.token(),
                guestInquiry("someone-" + UUID.randomUUID() + "@example.com"))
                .andExpect(status().isCreated()).andReturn();

        UUID id = UUID.fromString(read(result).at("/data/id").asText());
        assertThat(inquiryRepository.findWithServiceById(id).orElseThrow().getCustomerProfile()).isNull();
    }

    // ---------- customers ----------

    @Test
    void customerWithProfileCanCreateAndListOwnInquiries() throws Exception {
        Account customer = createCustomerWithProfile();

        MvcResult created = send(HttpMethod.POST, "/api/v1/customer/inquiries", customer.token(),
                Map.of("serviceSlug", "cybersecurity", "message", MESSAGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.serviceSlug").value("cybersecurity"))
                .andExpect(jsonPath("$.data.serviceName").value("Cybersecurity"))
                .andExpect(jsonPath("$.data.message").value(MESSAGE))
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andReturn();
        String id = read(created).at("/data/id").asText();

        send(HttpMethod.GET, "/api/v1/customer/inquiries", customer.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id));
        send(HttpMethod.GET, "/api/v1/customer/inquiries/" + id, customer.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void customerInquiryTakesContactDetailsFromTheAccountNotTheRequest() throws Exception {
        Account customer = createCustomerWithProfile();
        Map<String, Object> body = new HashMap<>();
        body.put("message", MESSAGE);
        body.put("name", "Forged Name");
        body.put("email", "forged@example.com");

        MvcResult created = send(HttpMethod.POST, "/api/v1/customer/inquiries", customer.token(), body)
                .andExpect(status().isCreated()).andReturn();

        UUID id = UUID.fromString(read(created).at("/data/id").asText());
        CustomerInquiry stored = inquiryRepository.findWithServiceById(id).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Test User");
        assertThat(stored.getEmail()).isEqualTo(customer.email());
        assertThat(stored.getPhone()).isEqualTo("+255 712 000 111");
        assertThat(stored.getCustomerProfile()).isNotNull();
    }

    @Test
    void customerWithoutProfileMustCreateOneFirst() throws Exception {
        Account customer = createAccount("CUSTOMER");

        send(HttpMethod.POST, "/api/v1/customer/inquiries", customer.token(), Map.of("message", MESSAGE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PROFILE_NOT_FOUND"));
        send(HttpMethod.GET, "/api/v1/customer/inquiries", customer.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PROFILE_NOT_FOUND"));
    }

    @Test
    void customersOnlySeeTheirOwnInquiries() throws Exception {
        Account customerA = createCustomerWithProfile();
        Account customerB = createCustomerWithProfile();
        String bInquiryId = read(send(HttpMethod.POST, "/api/v1/customer/inquiries", customerB.token(),
                Map.of("message", "Customer B private inquiry text."))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();

        send(HttpMethod.GET, "/api/v1/customer/inquiries", customerA.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        send(HttpMethod.GET, "/api/v1/customer/inquiries/" + bInquiryId, customerA.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INQUIRY_NOT_FOUND"));
    }

    @Test
    void inquiryEndpointsRequireTheCustomerRole() throws Exception {
        Account student = createAccount("STUDENT");

        send(HttpMethod.GET, "/api/v1/customer/inquiries", student.token(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        send(HttpMethod.POST, "/api/v1/customer/inquiries", student.token(), Map.of("message", MESSAGE))
                .andExpect(status().isForbidden());
        send(HttpMethod.GET, "/api/v1/customer/inquiries", null, null)
                .andExpect(status().isUnauthorized());
    }

    // ---------- staff ----------

    @Test
    void adminCanListFilterReadAndUpdateInquiries() throws Exception {
        String admin = createAdminToken();
        String email = "guest-" + UUID.randomUUID() + "@example.com";
        String id = read(send(HttpMethod.POST, "/api/v1/inquiries", null, guestInquiry(email))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asText();

        send(HttpMethod.GET, "/api/v1/admin/inquiries?status=NEW&size=100", admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + id + "')].email").value(email))
                .andExpect(jsonPath("$.data.page").value(0));
        send(HttpMethod.GET, "/api/v1/admin/inquiries/" + id, admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Guest Visitor"))
                .andExpect(jsonPath("$.data.customerProfileId").doesNotExist())
                .andExpect(jsonPath("$.data.serviceName").value("Web Development"));

        send(HttpMethod.PUT, "/api/v1/admin/inquiries/" + id + "/status", admin, Map.of("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
        send(HttpMethod.GET, "/api/v1/admin/inquiries?status=NEW&size=100", admin, null)
                .andExpect(jsonPath("$.data.items[?(@.id == '" + id + "')]").isEmpty());
    }

    @Test
    void adminListIsPaged() throws Exception {
        String admin = createAdminToken();
        for (int i = 0; i < 2; i++) {
            send(HttpMethod.POST, "/api/v1/inquiries", null, guestInquiry("page-" + UUID.randomUUID() + "@example.com"))
                    .andExpect(status().isCreated());
        }

        send(HttpMethod.GET, "/api/v1/admin/inquiries?page=0&size=1", admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalItems").isNumber())
                .andExpect(jsonPath("$.data.totalPages").isNumber());
    }

    @Test
    void unknownInquiryReturnsNotFoundForAdmin() throws Exception {
        send(HttpMethod.GET, "/api/v1/admin/inquiries/" + UUID.randomUUID(), createAdminToken(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INQUIRY_NOT_FOUND"));
    }

    @Test
    void adminInquiryEndpointsAreForAdminsOnly() throws Exception {
        Account customer = createCustomerWithProfile();
        Account student = createAccount("STUDENT");

        send(HttpMethod.GET, "/api/v1/admin/inquiries", customer.token(), null).andExpect(status().isForbidden());
        send(HttpMethod.GET, "/api/v1/admin/inquiries", student.token(), null).andExpect(status().isForbidden());
        send(HttpMethod.GET, "/api/v1/admin/inquiries", null, null).andExpect(status().isUnauthorized());
        send(HttpMethod.PUT, "/api/v1/admin/inquiries/" + UUID.randomUUID() + "/status", customer.token(),
                Map.of("status", "RESOLVED")).andExpect(status().isForbidden());
    }

    @Test
    void invalidStatusValueIsRejected() throws Exception {
        send(HttpMethod.PUT, "/api/v1/admin/inquiries/" + UUID.randomUUID() + "/status", createAdminToken(),
                Map.of("status", "ARCHIVED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
