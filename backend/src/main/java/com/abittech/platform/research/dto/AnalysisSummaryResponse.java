package com.abittech.platform.research.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.research.entity.ResearchAnalysis;

public record AnalysisSummaryResponse(
        UUID id, String projectTitle, String similarityLevel, int papersFound, Instant createdAt) {

    public static AnalysisSummaryResponse from(ResearchAnalysis analysis) {
        return new AnalysisSummaryResponse(analysis.getId(), analysis.getProjectTitle(), analysis.getSimilarityLevel(),
                analysis.getPapersFound(), analysis.getCreatedAt());
    }
}
