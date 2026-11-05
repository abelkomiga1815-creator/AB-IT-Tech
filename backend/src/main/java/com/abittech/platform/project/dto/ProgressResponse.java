package com.abittech.platform.project.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.project.entity.ProjectProgress;
import com.abittech.platform.project.entity.ProjectStage;

public record ProgressResponse(UUID id, ProjectStage stage, int percentComplete, String note, Instant createdAt) {

    public static ProgressResponse from(ProjectProgress progress) {
        return new ProgressResponse(progress.getId(), progress.getStage(), progress.getPercentComplete(),
                progress.getNote(), progress.getCreatedAt());
    }
}
