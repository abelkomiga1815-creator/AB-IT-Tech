package com.abittech.platform.project.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.abittech.platform.project.entity.Project;
import com.abittech.platform.project.entity.ProjectStage;
import com.abittech.platform.project.entity.ProjectStatus;

public record ProjectResponse(
        UUID id,
        String title,
        String description,
        String researchArea,
        String supervisorName,
        LocalDate deadline,
        ProjectStatus status,
        ProjectStage currentStage,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getResearchArea(),
                project.getSupervisorName(),
                project.getDeadline(),
                project.getStatus(),
                project.getCurrentStage(),
                project.getProgressPercent(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
