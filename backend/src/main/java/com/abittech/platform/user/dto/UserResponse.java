package com.abittech.platform.user.dto;

import java.util.UUID;

import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;

/**
 * Safe, public view of an account. Never contains the password hash.
 */
public record UserResponse(UUID id, String fullName, String email, Role role, boolean enabled) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.isEnabled());
    }
}
