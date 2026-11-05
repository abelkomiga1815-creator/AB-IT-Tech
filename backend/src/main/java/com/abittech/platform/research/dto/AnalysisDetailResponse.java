package com.abittech.platform.research.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A saved analysis with its full report exactly as produced by the AI engine
 * (retrieved papers, similarity indicator, gap suggestions, notices).
 */
public record AnalysisDetailResponse(
        UUID id, String projectTitle, String similarityLevel, int papersFound, Instant createdAt, JsonNode report) {
}
