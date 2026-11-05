package com.abittech.platform.customer.controller;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.customer.dto.CustomerProfileRequest;
import com.abittech.platform.customer.dto.CustomerProfileResponse;
import com.abittech.platform.customer.dto.CustomerProfileUpdateRequest;
import com.abittech.platform.customer.service.CustomerProfileService;
import com.abittech.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated customer's own profile. Role CUSTOMER is enforced in {@code SecurityConfig};
 * the profile is always resolved from the authenticated identity, never from an id in the request.
 */
@RestController
@RequestMapping("/api/v1/customer/profile")
public class CustomerProfileController {

    private final CustomerProfileService service;

    public CustomerProfileController(CustomerProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<CustomerProfileResponse> get(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(service.getMyProfile(currentUser.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerProfileResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @Valid @RequestBody CustomerProfileRequest request) {
        return ApiResponse.ok(service.create(currentUser.id(), request));
    }

    @PutMapping
    public ApiResponse<CustomerProfileResponse> update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ApiResponse.ok(service.update(currentUser.id(), request));
    }
}
