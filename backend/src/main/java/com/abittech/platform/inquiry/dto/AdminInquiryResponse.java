package com.abittech.platform.inquiry.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.inquiry.entity.CustomerInquiry;
import com.abittech.platform.inquiry.entity.InquiryStatus;

/**
 * Staff view of an inquiry, including the contact details needed to answer it.
 *
 * @param customerProfileId {@code null} for guest inquiries
 */
public record AdminInquiryResponse(
        UUID id,
        UUID customerProfileId,
        String name,
        String email,
        String phone,
        String serviceSlug,
        String serviceName,
        String message,
        InquiryStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminInquiryResponse from(CustomerInquiry inquiry) {
        return new AdminInquiryResponse(
                inquiry.getId(),
                inquiry.getCustomerProfile() == null ? null : inquiry.getCustomerProfile().getId(),
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getPhone(),
                inquiry.getService() == null ? null : inquiry.getService().getSlug(),
                inquiry.getService() == null ? null : inquiry.getService().getName(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt());
    }
}
