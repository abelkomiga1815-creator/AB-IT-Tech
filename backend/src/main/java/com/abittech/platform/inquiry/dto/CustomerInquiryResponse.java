package com.abittech.platform.inquiry.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.inquiry.entity.CustomerInquiry;
import com.abittech.platform.inquiry.entity.InquiryStatus;

public record CustomerInquiryResponse(
        UUID id,
        String serviceSlug,
        String serviceName,
        String message,
        InquiryStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerInquiryResponse from(CustomerInquiry inquiry) {
        return new CustomerInquiryResponse(
                inquiry.getId(),
                inquiry.getService() == null ? null : inquiry.getService().getSlug(),
                inquiry.getService() == null ? null : inquiry.getService().getName(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt());
    }
}
