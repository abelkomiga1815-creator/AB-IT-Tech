package com.abittech.platform.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByStudentProfileIdOrderByCreatedAtDesc(UUID studentProfileId);

    Optional<Project> findByIdAndStudentProfileId(UUID id, UUID studentProfileId);
}
