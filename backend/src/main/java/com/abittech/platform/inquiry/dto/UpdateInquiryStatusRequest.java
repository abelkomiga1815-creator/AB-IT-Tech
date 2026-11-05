package com.abittech.platform.inquiry.dto;

import com.abittech.platform.inquiry.entity.InquiryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInquiryStatusRequest(@NotNull InquiryStatus status) {
}
