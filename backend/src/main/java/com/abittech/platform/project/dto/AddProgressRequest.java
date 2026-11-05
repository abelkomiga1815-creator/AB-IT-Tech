package com.abittech.platform.project.dto;

import com.abittech.platform.project.entity.ProjectStage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddProgressRequest(
        @NotNull ProjectStage stage,
        @Min(0) @Max(100) int percentComplete,
        @Size(max = 2000) String note) {
}
