package com.abittech.platform.inquiry.controller;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.inquiry.dto.GuestInquiryRequest;
import com.abittech.platform.inquiry.dto.InquiryReceiptResponse;
import com.abittech.platform.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint behind the website contact and request forms. No login required.
 */
@RestController
@RequestMapping("/api/v1/inquiries")
public class GuestInquiryController {

    private final InquiryService inquiryService;

    public GuestInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InquiryReceiptResponse> create(@Valid @RequestBody GuestInquiryRequest request) {
        return ApiResponse.ok(inquiryService.createGuestInquiry(request));
    }
}
