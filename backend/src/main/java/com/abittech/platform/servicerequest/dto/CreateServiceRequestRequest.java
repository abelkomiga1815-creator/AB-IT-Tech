package com.abittech.platform.servicerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestRequest(
        @NotBlank @Size(max = 100) String serviceSlug,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(min = 10, max = 4000) String description) {
}
