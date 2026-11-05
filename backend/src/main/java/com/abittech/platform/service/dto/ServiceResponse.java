package com.abittech.platform.service.dto;

import com.abittech.platform.service.entity.ServiceOffering;

public record ServiceResponse(String slug, String name, String summary, String description) {

    public static ServiceResponse from(ServiceOffering service) {
        return new ServiceResponse(service.getSlug(), service.getName(), service.getSummary(), service.getDescription());
    }
}
