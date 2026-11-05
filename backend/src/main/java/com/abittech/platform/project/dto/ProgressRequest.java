package com.abittech.platform.project.dto;

import com.abittech.platform.project.entity.ProjectStage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProgressRequest(
        @NotNull ProjectStage stage,
        @NotNull @Min(0) @Max(100) Integer percentComplete,
        @Size(max = 2000) String note) {
}
