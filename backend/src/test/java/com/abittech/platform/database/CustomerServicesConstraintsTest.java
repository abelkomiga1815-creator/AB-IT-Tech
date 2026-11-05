package com.abittech.platform.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.abittech.platform.customer.entity.CustomerProfile;
import com.abittech.platform.customer.entity.CustomerType;
import com.abittech.platform.customer.repository.CustomerProfileRepository;
import com.abittech.platform.inquiry.entity.CustomerInquiry;
import com.abittech.platform.inquiry.repository.CustomerInquiryRepository;
import com.abittech.platform.service.entity.ServiceOffering;
import com.abittech.platform.service.repository.ServiceOfferingRepository;
import com.abittech.platform.servicerequest.entity.ServiceRequest;
import com.abittech.platform.servicerequest.repository.ServiceRequestRepository;
import com.abittech.platform.support.IntegrationTestBase;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database-level guarantees of the services, inquiries and service-request tables.
 */
class CustomerServicesConstraintsTest extends IntegrationTestBase {

    @Autowired
    private CustomerProfileRepository customerProfileRepository;
    @Autowired
    private ServiceOfferingRepository serviceRepository;
    @Autowired
    private CustomerInquiryRepository inquiryRepository;
    @Autowired
    private ServiceRequestRepository requestRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private CustomerProfile newProfile() {
        User user = userRepository.saveAndFlush(
                new User("DB Customer", "cs-" + UUID.randomUUID() + "@example.com", "hash", Role.CUSTOMER));
        CustomerProfile profile = new CustomerProfile(user);
        profile.setCustomerType(CustomerType.INDIVIDUAL);
        return customerProfileRepository.saveAndFlush(profile);
    }

    private ServiceOffering newService() {
        return serviceRepository.saveAndFlush(new ServiceOffering(
                "svc-" + UUID.randomUUID(), "Service " + UUID.randomUUID(), "Summary", "Description", 50));
    }

    @Test
    void serviceSlugAndNameMustBeUnique() {
        ServiceOffering existing = newService();
        assertThatThrownBy(() -> serviceRepository.saveAndFlush(
                new ServiceOffering(existing.getSlug(), "Another name " + UUID.randomUUID(), "S", "D", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> serviceRepository.saveAndFlush(
                new ServiceOffering("another-" + UUID.randomUUID(), existing.getName(), "S", "D", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void serviceSlugMustBeLowerCase() {
        assertThatThrownBy(() -> serviceRepository.saveAndFlush(
                new ServiceOffering("Upper-" + UUID.randomUUID(), "Name " + UUID.randomUUID(), "S", "D", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void inquiryStatusMustBeKnown() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO customer_inquiries (id, name, email, message, status) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), "N", "n@example.com", "A long enough message", "ARCHIVED"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guestInquiryNeedsNoProfileOrService() {
        CustomerInquiry saved = inquiryRepository.saveAndFlush(
                new CustomerInquiry("Guest", "guest@example.com", null, "A long enough message."));
        assertThat(inquiryRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void serviceRequestStatusMustBeKnown() {
        CustomerProfile profile = newProfile();
        ServiceOffering service = newService();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO service_requests (id, customer_profile_id, service_id, title, description, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), profile.getId(), service.getId(), "T", "A long enough description", "DONE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void serviceRequestNeedsExistingProfileAndService() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO service_requests (id, customer_profile_id, service_id, title, description) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "T", "A long enough description"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void referencedServicesAndProfilesCannotBeDeleted() {
        CustomerProfile profile = newProfile();
        ServiceOffering service = newService();
        requestRepository.saveAndFlush(new ServiceRequest(profile, service, "Title", "A long enough description"));
        inquiryRepository.saveAndFlush(inquiryFor(profile, service));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM services WHERE id = ?", service.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM customer_profiles WHERE id = ?", profile.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(serviceRepository.existsById(service.getId())).isTrue();
        assertThat(customerProfileRepository.existsById(profile.getId())).isTrue();
    }

    private CustomerInquiry inquiryFor(CustomerProfile profile, ServiceOffering service) {
        CustomerInquiry inquiry = new CustomerInquiry("DB Customer", "cs@example.com", null, "A long enough message.");
        inquiry.setCustomerProfile(profile);
        inquiry.setService(service);
        return inquiry;
    }
}
