package com.abittech.platform.servicerequest.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.servicerequest.entity.ServiceRequest;
import com.abittech.platform.servicerequest.entity.ServiceRequestStatus;

/**
 * Staff view of a service request, including who asked for it.
 */
public record AdminServiceRequestResponse(
        UUID id,
        UUID customerProfileId,
        String customerFullName,
        String customerEmail,
        String organizationName,
        String serviceSlug,
        String serviceName,
        String title,
        String description,
        ServiceRequestStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminServiceRequestResponse from(ServiceRequest request) {
        return new AdminServiceRequestResponse(
                request.getId(),
                request.getCustomerProfile().getId(),
                request.getCustomerProfile().getUser().getFullName(),
                request.getCustomerProfile().getUser().getEmail(),
                request.getCustomerProfile().getOrganizationName(),
                request.getService().getSlug(),
                request.getService().getName(),
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
