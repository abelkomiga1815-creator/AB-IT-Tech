package com.abittech.platform.inquiry.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.inquiry.entity.CustomerInquiry;
import com.abittech.platform.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerInquiryRepository extends JpaRepository<CustomerInquiry, UUID> {

    @EntityGraph(attributePaths = "service")
    List<CustomerInquiry> findByCustomerProfileIdOrderByCreatedAtDesc(UUID customerProfileId);

    @EntityGraph(attributePaths = "service")
    Optional<CustomerInquiry> findByIdAndCustomerProfileId(UUID id, UUID customerProfileId);

    @EntityGraph(attributePaths = "service")
    Optional<CustomerInquiry> findWithServiceById(UUID id);

    @EntityGraph(attributePaths = "service")
    Page<CustomerInquiry> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "service")
    Page<CustomerInquiry> findByStatus(InquiryStatus status, Pageable pageable);
}
