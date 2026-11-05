package com.abittech.platform.auth.dto;

import com.abittech.platform.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param role optional; {@code STUDENT} or {@code CUSTOMER} (default {@code CUSTOMER}).
 *             {@code ADMIN} can never be chosen at registration.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes, so longer passwords are rejected instead of silently truncated.
        @NotBlank @Size(min = 8, max = 72) String password,
        Role role) {
}
