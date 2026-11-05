package com.abittech.platform.defense.controller;

import java.util.List;
import java.util.UUID;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.defense.dto.DefenseQuestionRequest;
import com.abittech.platform.defense.dto.DefenseQuestionResponse;
import com.abittech.platform.defense.service.DefenseQuestionService;
import com.abittech.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/projects/{projectId}/defense-questions")
public class DefenseQuestionController {

    private final DefenseQuestionService service;

    public DefenseQuestionController(DefenseQuestionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DefenseQuestionResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @PathVariable UUID projectId,
                                                       @Valid @RequestBody DefenseQuestionRequest request) {
        return ApiResponse.ok(service.create(currentUser.id(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<DefenseQuestionResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                           @PathVariable UUID projectId) {
        return ApiResponse.ok(service.list(currentUser.id(), projectId));
    }

    @PutMapping("/{questionId}")
    public ApiResponse<DefenseQuestionResponse> update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @PathVariable UUID projectId,
                                                       @PathVariable UUID questionId,
                                                       @Valid @RequestBody DefenseQuestionRequest request) {
        return ApiResponse.ok(service.update(currentUser.id(), projectId, questionId, request));
    }

    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable UUID projectId,
                                    @PathVariable UUID questionId) {
        service.delete(currentUser.id(), projectId, questionId);
        return ApiResponse.ok();
    }
}
