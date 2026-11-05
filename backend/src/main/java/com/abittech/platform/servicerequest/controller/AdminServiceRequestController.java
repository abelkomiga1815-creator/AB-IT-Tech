package com.abittech.platform.servicerequest.controller;

import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.common.response.PageResponse;
import com.abittech.platform.servicerequest.dto.AdminServiceRequestResponse;
import com.abittech.platform.servicerequest.dto.UpdateServiceRequestStatusRequest;
import com.abittech.platform.servicerequest.entity.ServiceRequestStatus;
import com.abittech.platform.servicerequest.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal staff access (role ADMIN, enforced in SecurityConfig): customers cannot move their own
 * requests through the lifecycle, so staff need to list and update them. Not an admin dashboard.
 */
@RestController
@RequestMapping("/api/v1/admin/service-requests")
public class AdminServiceRequestController {

    private final ServiceRequestService service;

    public AdminServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminServiceRequestResponse>> list(
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.adminList(status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminServiceRequestResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.adminGet(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AdminServiceRequestResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateServiceRequestStatusRequest request) {
        return ApiResponse.ok(service.adminUpdateStatus(id, request.status()));
    }
}
