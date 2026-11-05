package com.abittech.platform.service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.service.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    List<ServiceOffering> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<ServiceOffering> findBySlugAndActiveTrue(String slug);
}
