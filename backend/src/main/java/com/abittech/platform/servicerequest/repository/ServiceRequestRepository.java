package com.abittech.platform.servicerequest.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.servicerequest.entity.ServiceRequest;
import com.abittech.platform.servicerequest.entity.ServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    @EntityGraph(attributePaths = "service")
    List<ServiceRequest> findByCustomerProfileIdOrderByCreatedAtDesc(UUID customerProfileId);

    @EntityGraph(attributePaths = "service")
    Optional<ServiceRequest> findByIdAndCustomerProfileId(UUID id, UUID customerProfileId);

    @EntityGraph(attributePaths = {"service", "customerProfile", "customerProfile.user"})
    Optional<ServiceRequest> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"service", "customerProfile", "customerProfile.user"})
    Page<ServiceRequest> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"service", "customerProfile", "customerProfile.user"})
    Page<ServiceRequest> findByStatus(ServiceRequestStatus status, Pageable pageable);
}
