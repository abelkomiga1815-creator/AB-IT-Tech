package com.abittech.platform.inquiry.controller;

import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.common.response.PageResponse;
import com.abittech.platform.inquiry.dto.AdminInquiryResponse;
import com.abittech.platform.inquiry.dto.UpdateInquiryStatusRequest;
import com.abittech.platform.inquiry.entity.InquiryStatus;
import com.abittech.platform.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal staff access (role ADMIN, enforced in SecurityConfig). It exists because guest inquiries
 * would otherwise be unreadable; it is not an admin dashboard.
 */
@RestController
@RequestMapping("/api/v1/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    public AdminInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminInquiryResponse>> list(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inquiryService.adminList(status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminInquiryResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(inquiryService.adminGet(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AdminInquiryResponse> updateStatus(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdateInquiryStatusRequest request) {
        return ApiResponse.ok(inquiryService.adminUpdateStatus(id, request.status()));
    }
}
