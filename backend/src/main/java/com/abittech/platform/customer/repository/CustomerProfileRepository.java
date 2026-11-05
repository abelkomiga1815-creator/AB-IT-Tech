package com.abittech.platform.customer.repository;

import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.customer.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    Optional<CustomerProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
