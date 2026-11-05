package com.abittech.platform.service.controller;

import java.util.List;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.service.dto.ServiceResponse;
import com.abittech.platform.service.service.ServiceCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public service catalogue used by the corporate website and the request forms.
 */
@RestController
@RequestMapping("/api/v1/services")
public class ServiceCatalogController {

    private final ServiceCatalogService catalogService;

    public ServiceCatalogController(ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiResponse<List<ServiceResponse>> list() {
        return ApiResponse.ok(catalogService.listActive());
    }

    @GetMapping("/{slug}")
    public ApiResponse<ServiceResponse> get(@PathVariable String slug) {
        return ApiResponse.ok(catalogService.getActiveBySlug(slug));
    }
}
