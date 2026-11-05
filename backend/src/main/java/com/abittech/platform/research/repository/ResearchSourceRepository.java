package com.abittech.platform.research.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.research.entity.ResearchSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchSourceRepository extends JpaRepository<ResearchSource, UUID> {

    List<ResearchSource> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ResearchSource> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectIdAndDoiIgnoreCase(UUID projectId, String doi);

    boolean existsByProjectIdAndTitleIgnoreCase(UUID projectId, String title);
}
