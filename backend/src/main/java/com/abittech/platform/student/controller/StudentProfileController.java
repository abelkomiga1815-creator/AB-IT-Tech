package com.abittech.platform.student.controller;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.security.AuthenticatedUser;
import com.abittech.platform.student.dto.StudentProfileRequest;
import com.abittech.platform.student.dto.StudentProfileResponse;
import com.abittech.platform.student.dto.StudentProfileUpdateRequest;
import com.abittech.platform.student.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated student's own profile. Role STUDENT is enforced in {@code SecurityConfig};
 * the profile is always resolved from the authenticated identity, never from an id in the request.
 */
@RestController
@RequestMapping("/api/v1/student/profile")
public class StudentProfileController {

    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<StudentProfileResponse> get(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(service.getMyProfile(currentUser.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentProfileResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                      @Valid @RequestBody StudentProfileRequest request) {
        return ApiResponse.ok(service.create(currentUser.id(), request));
    }

    @PutMapping
    public ApiResponse<StudentProfileResponse> update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                      @Valid @RequestBody StudentProfileUpdateRequest request) {
        return ApiResponse.ok(service.update(currentUser.id(), request));
    }
}
