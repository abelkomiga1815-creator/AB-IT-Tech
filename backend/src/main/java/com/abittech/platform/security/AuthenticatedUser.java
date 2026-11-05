package com.abittech.platform.security;

import java.util.UUID;

import com.abittech.platform.user.entity.Role;

/**
 * The authenticated principal placed in the security context for every request.
 * Controllers receive it with {@code @AuthenticationPrincipal}; ownership of data is always derived
 * from {@link #id()}, never from an id sent by the client.
 */
public record AuthenticatedUser(UUID id, String email, Role role) {
}
