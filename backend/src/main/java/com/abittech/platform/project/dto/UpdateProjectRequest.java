package com.abittech.platform.project.dto;

import java.time.LocalDate;

import com.abittech.platform.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/v1/student/projects/{id}}: the full set of editable fields (replace semantics).
 */
public record UpdateProjectRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 4000) String description,
        @Size(max = 150) String researchArea,
        @Size(max = 150) String supervisorName,
        LocalDate deadline,
        @NotNull ProjectStatus status) implements ProjectData {
}
