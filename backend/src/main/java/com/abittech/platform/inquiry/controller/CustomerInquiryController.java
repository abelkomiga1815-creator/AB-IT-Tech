package com.abittech.platform.inquiry.controller;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.inquiry.dto.CustomerInquiryRequest;
import com.abittech.platform.inquiry.dto.CustomerInquiryResponse;
import com.abittech.platform.inquiry.service.InquiryService;
import com.abittech.platform.security.AuthenticatedUser;
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
 * A logged-in customer's own inquiries (role CUSTOMER, customer profile required).
 */
@RestController
@RequestMapping("/api/v1/customer/inquiries")
public class CustomerInquiryController {

    private final InquiryService inquiryService;

    public CustomerInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerInquiryResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @Valid @RequestBody CustomerInquiryRequest request) {
        return ApiResponse.ok(inquiryService.createCustomerInquiry(currentUser.id(), request));
    }

    @GetMapping
    public ApiResponse<List<CustomerInquiryResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(inquiryService.listForCustomer(currentUser.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerInquiryResponse> get(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                    @PathVariable UUID id) {
        return ApiResponse.ok(inquiryService.getForCustomer(currentUser.id(), id));
    }
}
