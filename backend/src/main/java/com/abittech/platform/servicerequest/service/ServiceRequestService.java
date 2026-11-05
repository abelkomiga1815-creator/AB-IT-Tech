package com.abittech.platform.servicerequest.service;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.response.PageResponse;
import com.abittech.platform.customer.entity.CustomerProfile;
import com.abittech.platform.customer.repository.CustomerProfileRepository;
import com.abittech.platform.service.entity.ServiceOffering;
import com.abittech.platform.service.service.ServiceCatalogService;
import com.abittech.platform.servicerequest.dto.AdminServiceRequestResponse;
import com.abittech.platform.servicerequest.dto.CreateServiceRequestRequest;
import com.abittech.platform.servicerequest.dto.ServiceRequestResponse;
import com.abittech.platform.servicerequest.entity.ServiceRequest;
import com.abittech.platform.servicerequest.entity.ServiceRequestStatus;
import com.abittech.platform.servicerequest.repository.ServiceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service request use cases: customers act on their own requests only; staff manage the lifecycle.
 */
@Service
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ServiceRequestRepository requestRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ServiceCatalogService catalogService;

    public ServiceRequestService(ServiceRequestRepository requestRepository,
                                 CustomerProfileRepository customerProfileRepository,
                                 ServiceCatalogService catalogService) {
        this.requestRepository = requestRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.catalogService = catalogService;
    }

    // ---------- customers ----------

    @Transactional
    public ServiceRequestResponse create(UUID userId, CreateServiceRequestRequest request) {
        CustomerProfile profile = requireProfile(userId);
        ServiceOffering service = catalogService.requireActive(request.serviceSlug());
        ServiceRequest saved = requestRepository.save(
                new ServiceRequest(profile, service, request.title().trim(), request.description().trim()));
        log.info("Service request created: requestId={}, userId={}, service={}",
                saved.getId(), userId, service.getSlug());
        return ServiceRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> listForCustomer(UUID userId) {
        CustomerProfile profile = requireProfile(userId);
        return requestRepository.findByCustomerProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(ServiceRequestResponse::from)
                .toList();
    }

    /** Another customer's request is reported as not found, so its existence is not revealed. */
    @Transactional(readOnly = true)
    public ServiceRequestResponse getForCustomer(UUID userId, UUID requestId) {
        return ServiceRequestResponse.from(findOwned(userId, requestId));
    }

    @Transactional
    public ServiceRequestResponse cancelByCustomer(UUID userId, UUID requestId) {
        ServiceRequest request = findOwned(userId, requestId);
        if (!request.getStatus().isCancellableByCustomer()) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "This request can no longer be cancelled because its status is " + request.getStatus());
        }
        request.setStatus(ServiceRequestStatus.CANCELLED);
        ServiceRequest saved = requestRepository.saveAndFlush(request);
        log.info("Service request cancelled by customer: requestId={}, userId={}", requestId, userId);
        return ServiceRequestResponse.from(saved);
    }

    // ---------- staff ----------

    @Transactional(readOnly = true)
    public PageResponse<AdminServiceRequestResponse> adminList(ServiceRequestStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ServiceRequest> result = status == null
                ? requestRepository.findAllBy(pageable)
                : requestRepository.findByStatus(status, pageable);
        return PageResponse.from(result.map(AdminServiceRequestResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminServiceRequestResponse adminGet(UUID requestId) {
        return AdminServiceRequestResponse.from(findForAdmin(requestId));
    }

    @Transactional
    public AdminServiceRequestResponse adminUpdateStatus(UUID requestId, ServiceRequestStatus target) {
        ServiceRequest request = findForAdmin(requestId);
        ServiceRequestStatus current = request.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot change status from " + current + " to " + target);
        }
        request.setStatus(target);
        ServiceRequest saved = requestRepository.saveAndFlush(request);
        log.info("Service request status changed: requestId={}, from={}, to={}", requestId, current, target);
        return AdminServiceRequestResponse.from(saved);
    }

    // ---------- helpers ----------

    private ServiceRequest findOwned(UUID userId, UUID requestId) {
        CustomerProfile profile = requireProfile(userId);
        return requestRepository.findByIdAndCustomerProfileId(requestId, profile.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));
    }

    private ServiceRequest findForAdmin(UUID requestId) {
        return requestRepository.findWithDetailsById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));
    }

    private CustomerProfile requireProfile(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }
}
