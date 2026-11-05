package com.abittech.platform.inquiry.service;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.response.PageResponse;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.customer.entity.CustomerProfile;
import com.abittech.platform.customer.repository.CustomerProfileRepository;
import com.abittech.platform.inquiry.dto.AdminInquiryResponse;
import com.abittech.platform.inquiry.dto.CustomerInquiryRequest;
import com.abittech.platform.inquiry.dto.CustomerInquiryResponse;
import com.abittech.platform.inquiry.dto.GuestInquiryRequest;
import com.abittech.platform.inquiry.dto.InquiryReceiptResponse;
import com.abittech.platform.inquiry.entity.CustomerInquiry;
import com.abittech.platform.inquiry.entity.InquiryStatus;
import com.abittech.platform.inquiry.repository.CustomerInquiryRepository;
import com.abittech.platform.service.entity.ServiceOffering;
import com.abittech.platform.service.service.ServiceCatalogService;
import com.abittech.platform.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inquiry use cases for guests, customers (own inquiries only) and staff.
 */
@Service
public class InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerInquiryRepository inquiryRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ServiceCatalogService catalogService;

    public InquiryService(CustomerInquiryRepository inquiryRepository,
                          CustomerProfileRepository customerProfileRepository,
                          ServiceCatalogService catalogService) {
        this.inquiryRepository = inquiryRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.catalogService = catalogService;
    }

    // ---------- guests ----------

    @Transactional
    public InquiryReceiptResponse createGuestInquiry(GuestInquiryRequest request) {
        CustomerInquiry inquiry = new CustomerInquiry(
                request.name().trim(),
                Strings.normalizeEmail(request.email()),
                Strings.trimToNull(request.phone()),
                request.message().trim());
        inquiry.setService(resolveOptionalService(request.serviceSlug()));
        CustomerInquiry saved = inquiryRepository.save(inquiry);
        log.info("Inquiry received: inquiryId={}, source=guest", saved.getId());
        return InquiryReceiptResponse.from(saved);
    }

    // ---------- customers ----------

    @Transactional
    public CustomerInquiryResponse createCustomerInquiry(UUID userId, CustomerInquiryRequest request) {
        CustomerProfile profile = requireProfile(userId);
        User user = profile.getUser();
        CustomerInquiry inquiry = new CustomerInquiry(
                user.getFullName(), user.getEmail(), profile.getPhone(), request.message().trim());
        inquiry.setCustomerProfile(profile);
        inquiry.setService(resolveOptionalService(request.serviceSlug()));
        CustomerInquiry saved = inquiryRepository.save(inquiry);
        log.info("Inquiry received: inquiryId={}, source=customer, userId={}", saved.getId(), userId);
        return CustomerInquiryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerInquiryResponse> listForCustomer(UUID userId) {
        CustomerProfile profile = requireProfile(userId);
        return inquiryRepository.findByCustomerProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(CustomerInquiryResponse::from)
                .toList();
    }

    /** Another customer's inquiry is reported as not found, so its existence is not revealed. */
    @Transactional(readOnly = true)
    public CustomerInquiryResponse getForCustomer(UUID userId, UUID inquiryId) {
        CustomerProfile profile = requireProfile(userId);
        return inquiryRepository.findByIdAndCustomerProfileId(inquiryId, profile.getId())
                .map(CustomerInquiryResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    // ---------- staff ----------

    @Transactional(readOnly = true)
    public PageResponse<AdminInquiryResponse> adminList(InquiryStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CustomerInquiry> result = status == null
                ? inquiryRepository.findAllBy(pageable)
                : inquiryRepository.findByStatus(status, pageable);
        return PageResponse.from(result.map(AdminInquiryResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminInquiryResponse adminGet(UUID inquiryId) {
        return AdminInquiryResponse.from(findForAdmin(inquiryId));
    }

    @Transactional
    public AdminInquiryResponse adminUpdateStatus(UUID inquiryId, InquiryStatus status) {
        CustomerInquiry inquiry = findForAdmin(inquiryId);
        inquiry.setStatus(status);
        CustomerInquiry saved = inquiryRepository.saveAndFlush(inquiry);
        log.info("Inquiry status changed: inquiryId={}, status={}", inquiryId, status);
        return AdminInquiryResponse.from(saved);
    }

    // ---------- helpers ----------

    private CustomerInquiry findForAdmin(UUID inquiryId) {
        return inquiryRepository.findWithServiceById(inquiryId)
                .orElseThrow(() -> new ApiException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    private CustomerProfile requireProfile(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }

    private ServiceOffering resolveOptionalService(String slug) {
        return Strings.trimToNull(slug) == null ? null : catalogService.requireActive(slug);
    }
}
