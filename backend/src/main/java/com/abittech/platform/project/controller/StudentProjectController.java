package com.abittech.platform.project.controller;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.project.dto.CreateProjectRequest;
import com.abittech.platform.project.dto.ProjectResponse;
import com.abittech.platform.project.dto.UpdateProjectRequest;
import com.abittech.platform.project.service.ProjectService;
import com.abittech.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated student's own projects (role STUDENT, student profile required).
 */
@RestController
@RequestMapping("/api/v1/student/projects")
public class StudentProjectController {

    private final ProjectService projectService;

    public StudentProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.ok(projectService.create(currentUser.id(), request));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(projectService.list(currentUser.id()));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                            @PathVariable UUID projectId) {
        return ApiResponse.ok(projectService.get(currentUser.id(), projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @PathVariable UUID projectId,
                                               @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.ok(projectService.update(currentUser.id(), projectId, request));
    }
}
