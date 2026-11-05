package com.abittech.platform.servicerequest.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.servicerequest.entity.ServiceRequest;
import com.abittech.platform.servicerequest.entity.ServiceRequestStatus;

/**
 * A customer's view of one of their own service requests.
 */
public record ServiceRequestResponse(
        UUID id,
        String serviceSlug,
        String serviceName,
        String title,
        String description,
        ServiceRequestStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ServiceRequestResponse from(ServiceRequest request) {
        return new ServiceRequestResponse(
                request.getId(),
                request.getService().getSlug(),
                request.getService().getName(),
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
