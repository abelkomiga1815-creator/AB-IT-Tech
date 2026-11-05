package com.abittech.platform.service.service;

import java.util.List;
import java.util.Locale;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.service.dto.ServiceResponse;
import com.abittech.platform.service.entity.ServiceOffering;
import com.abittech.platform.service.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to the service catalogue. Only active services are visible.
 */
@Service
public class ServiceCatalogService {

    private final ServiceOfferingRepository repository;

    public ServiceCatalogService(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> listActive() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc().stream().map(ServiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getActiveBySlug(String slug) {
        return ServiceResponse.from(requireActive(slug));
    }

    /**
     * Resolves an active service by slug for use by inquiries and service requests.
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} when the slug is unknown or the service is inactive
     */
    @Transactional(readOnly = true)
    public ServiceOffering requireActive(String slug) {
        String normalized = Strings.trimToNull(slug);
        if (normalized == null) {
            throw new ApiException(ErrorCode.SERVICE_NOT_FOUND);
        }
        return repository.findBySlugAndActiveTrue(normalized.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
    }
}
