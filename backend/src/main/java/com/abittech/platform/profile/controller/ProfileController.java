package com.abittech.platform.profile.controller;

import com.abittech.platform.common.response.ApiResponse;
import com.abittech.platform.profile.dto.UpdateProfileRequest;
import com.abittech.platform.security.AuthenticatedUser;
import com.abittech.platform.user.dto.UserResponse;
import com.abittech.platform.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Basic account profile of the authenticated user. Always operates on the caller's own account.
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserResponse> get(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(UserResponse.from(userService.getById(currentUser.id())));
    }

    @PutMapping
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(UserResponse.from(userService.updateFullName(currentUser.id(), request.fullName())));
    }
}
