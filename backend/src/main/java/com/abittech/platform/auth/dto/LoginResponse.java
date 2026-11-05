package com.abittech.platform.auth.dto;

import com.abittech.platform.user.dto.UserResponse;

/**
 * @param accessToken JWT to send as {@code Authorization: Bearer <accessToken>}
 * @param tokenType   always {@code Bearer}
 * @param expiresIn   token lifetime in seconds
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
