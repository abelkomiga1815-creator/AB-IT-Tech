package com.abittech.platform.document.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.document.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {

    List<ProjectDocument> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ProjectDocument> findByIdAndProjectId(UUID id, UUID projectId);
}
