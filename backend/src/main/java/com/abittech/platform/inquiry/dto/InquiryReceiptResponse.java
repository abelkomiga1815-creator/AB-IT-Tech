package com.abittech.platform.inquiry.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.inquiry.entity.CustomerInquiry;
import com.abittech.platform.inquiry.entity.InquiryStatus;

/**
 * Minimal confirmation returned to anonymous visitors: no personal data is echoed back.
 */
public record InquiryReceiptResponse(UUID id, InquiryStatus status, Instant createdAt) {

    public static InquiryReceiptResponse from(CustomerInquiry inquiry) {
        return new InquiryReceiptResponse(inquiry.getId(), inquiry.getStatus(), inquiry.getCreatedAt());
    }
}
