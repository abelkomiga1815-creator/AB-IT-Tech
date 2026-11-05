package com.abittech.platform.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/customer/inquiries}. Name, e-mail and phone come from the customer's
 * account and profile, never from the request.
 */
public record CustomerInquiryRequest(
        @Size(max = 100) String serviceSlug,
        @NotBlank @Size(min = 10, max = 4000) String message) {
}
