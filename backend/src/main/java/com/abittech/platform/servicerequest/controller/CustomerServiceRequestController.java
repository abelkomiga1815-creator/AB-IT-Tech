package com.abittech.platform.servicerequest.controller;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.security.AuthenticatedUser;
import com.abittech.platform.servicerequest.dto.CreateServiceRequestRequest;
import com.abittech.platform.servicerequest.dto.ServiceRequestResponse;
import com.abittech.platform.servicerequest.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * A logged-in customer's own service requests (role CUSTOMER, customer profile required).
 */
@RestController
@RequestMapping("/api/v1/customer/service-requests")
public class CustomerServiceRequestController {

    private final ServiceRequestService service;

    public CustomerServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ServiceRequestResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                      @Valid @RequestBody CreateServiceRequestRequest request) {
        return ApiResponse.ok(service.create(currentUser.id(), request));
    }

    @GetMapping
    public ApiResponse<List<ServiceRequestResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(service.listForCustomer(currentUser.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceRequestResponse> get(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable UUID id) {
        return ApiResponse.ok(service.getForCustomer(currentUser.id(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ServiceRequestResponse> cancel(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                      @PathVariable UUID id) {
        return ApiResponse.ok(service.cancelByCustomer(currentUser.id(), id));
    }
}
