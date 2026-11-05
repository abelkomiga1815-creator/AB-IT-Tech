package com.abittech.platform.project.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/student/projects}. Only the title is mandatory; the status starts as PLANNING.
 */
public record CreateProjectRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 4000) String description,
        @Size(max = 150) String researchArea,
        @Size(max = 150) String supervisorName,
        LocalDate deadline) implements ProjectData {
}
