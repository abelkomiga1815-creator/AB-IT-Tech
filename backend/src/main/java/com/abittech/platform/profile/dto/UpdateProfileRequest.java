package com.abittech.platform.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The only field an ordinary user may change through {@code PUT /api/v1/profile}.
 * Any other property in the request body (id, email, role, enabled, password) is ignored because
 * this DTO does not declare it.
 */
public record UpdateProfileRequest(@NotBlank @Size(max = 150) String fullName) {
}
