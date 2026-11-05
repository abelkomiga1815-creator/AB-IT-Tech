package com.abittech.platform.project.controller;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.project.dto.AddProgressRequest;
import com.abittech.platform.project.dto.ProgressResponse;
import com.abittech.platform.project.service.ProjectProgressService;
import com.abittech.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/projects/{projectId}/progress")
public class ProjectProgressController {

    private final ProjectProgressService progressService;

    public ProjectProgressController(ProjectProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProgressResponse> add(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @PathVariable UUID projectId,
                                             @Valid @RequestBody AddProgressRequest request) {
        return ApiResponse.ok(progressService.add(currentUser.id(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<ProgressResponse>> history(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @PathVariable UUID projectId) {
        return ApiResponse.ok(progressService.history(currentUser.id(), projectId));
    }
}
