package com.abittech.platform.servicerequest.entity;

import com.abittech.platform.common.persistence.AuditedEntity;
import com.abittech.platform.customer.entity.CustomerProfile;
import com.abittech.platform.service.entity.ServiceOffering;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A customer's request for a service: the customer-side "project". A customer profile can have many.
 */
@Entity
@Table(name = "service_requests")
public class ServiceRequest extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false, updatable = false)
    private CustomerProfile customerProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private ServiceOffering service;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceRequestStatus status = ServiceRequestStatus.SUBMITTED;

    protected ServiceRequest() {
        // required by JPA
    }

    public ServiceRequest(CustomerProfile customerProfile, ServiceOffering service, String title, String description) {
        this.customerProfile = customerProfile;
        this.service = service;
        this.title = title;
        this.description = description;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public ServiceOffering getService() {
        return service;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
    }
}
