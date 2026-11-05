package com.abittech.platform.customer.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.customer.entity.CustomerProfile;
import com.abittech.platform.customer.entity.CustomerType;

public record CustomerProfileResponse(
        UUID id,
        CustomerType customerType,
        String organizationName,
        String phone,
        String address,
        String city,
        String country,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerProfileResponse from(CustomerProfile profile) {
        return new CustomerProfileResponse(
                profile.getId(),
                profile.getCustomerType(),
                profile.getOrganizationName(),
                profile.getPhone(),
                profile.getAddress(),
                profile.getCity(),
                profile.getCountry(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
