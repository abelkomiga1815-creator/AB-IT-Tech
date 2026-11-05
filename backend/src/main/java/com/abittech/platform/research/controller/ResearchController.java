package com.abittech.platform.research.controller;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.research.dto.AnalysisDetailResponse;
import com.abittech.platform.research.dto.AnalysisSummaryResponse;
import com.abittech.platform.research.dto.SaveSourceRequest;
import com.abittech.platform.research.dto.SourceResponse;
import com.abittech.platform.research.service.ResearchService;
import com.abittech.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Research assistance for one of the student's own projects (role STUDENT via SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/student/projects/{projectId}/research")
public class ResearchController {

    private final ResearchService researchService;

    public ResearchController(ResearchService researchService) {
        this.researchService = researchService;
    }

    /** Runs a new analysis; can take up to a minute because external sources are queried. */
    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnalysisDetailResponse> run(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable UUID projectId) {
        return ApiResponse.ok(researchService.runAnalysis(currentUser.id(), projectId));
    }

    @GetMapping("/analyses")
    public ApiResponse<List<AnalysisSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                           @PathVariable UUID projectId) {
        return ApiResponse.ok(researchService.listAnalyses(currentUser.id(), projectId));
    }

    @GetMapping("/analyses/{analysisId}")
    public ApiResponse<AnalysisDetailResponse> get(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable UUID projectId,
                                                   @PathVariable UUID analysisId) {
        return ApiResponse.ok(researchService.getAnalysis(currentUser.id(), projectId, analysisId));
    }

    @PostMapping("/sources")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SourceResponse> saveSource(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                  @PathVariable UUID projectId,
                                                  @Valid @RequestBody SaveSourceRequest request) {
        return ApiResponse.ok(researchService.saveSource(currentUser.id(), projectId, request));
    }

    @GetMapping("/sources")
    public ApiResponse<List<SourceResponse>> sources(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable UUID projectId) {
        return ApiResponse.ok(researchService.listSources(currentUser.id(), projectId));
    }

    @DeleteMapping("/sources/{sourceId}")
    public ApiResponse<Void> deleteSource(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                          @PathVariable UUID projectId,
                                          @PathVariable UUID sourceId) {
        researchService.deleteSource(currentUser.id(), projectId, sourceId);
        return ApiResponse.ok();
    }
}
