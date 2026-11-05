package com.abittech.platform.project.repository;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.project.entity.ProjectProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectProgressRepository extends JpaRepository<ProjectProgress, UUID> {

    List<ProjectProgress> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
