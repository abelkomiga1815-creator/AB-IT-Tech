package com.abittech.platform.servicerequest.dto;

import com.abittech.platform.servicerequest.entity.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateServiceRequestStatusRequest(@NotNull ServiceRequestStatus status) {
}
