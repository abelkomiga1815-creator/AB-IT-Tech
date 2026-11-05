package com.abittech.platform.customer.dto;

import com.abittech.platform.common.validation.ValidationPatterns;
import com.abittech.platform.customer.entity.CustomerType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/v1/customer/profile}: the full set of editable fields (replace semantics).
 */
public record CustomerProfileUpdateRequest(
        @NotNull CustomerType customerType,
        @Size(max = 200) String organizationName,
        @Pattern(regexp = ValidationPatterns.PHONE, message = "must be a valid phone number") String phone,
        @Size(max = 300) String address,
        @Size(max = 100) String city,
        @Size(max = 100) String country) implements CustomerProfileData {
}
