package com.abittech.platform.inquiry.dto;

import com.abittech.platform.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/inquiries} (public, used by the website contact form).
 *
 * @param serviceSlug optional slug of the service the visitor is interested in
 */
public record GuestInquiryRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = ValidationPatterns.PHONE, message = "must be a valid phone number") String phone,
        @Size(max = 100) String serviceSlug,
        @NotBlank @Size(min = 10, max = 4000) String message) {
}
