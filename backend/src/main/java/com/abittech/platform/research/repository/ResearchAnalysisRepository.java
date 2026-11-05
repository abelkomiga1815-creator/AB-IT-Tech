package com.abittech.platform.research.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.research.entity.ResearchAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchAnalysisRepository extends JpaRepository<ResearchAnalysis, UUID> {

    List<ResearchAnalysis> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ResearchAnalysis> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<ResearchAnalysis> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
