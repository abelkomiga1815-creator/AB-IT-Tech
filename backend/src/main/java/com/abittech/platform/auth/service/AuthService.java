package com.abittech.platform.auth.service;

import com.abittech.platform.auth.dto.LoginRequest;
import com.abittech.platform.auth.dto.LoginResponse;
import com.abittech.platform.auth.dto.RegisterRequest;
import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.security.JwtService;
import com.abittech.platform.user.dto.UserResponse;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Registration and login use cases.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Used to spend the same hashing time when the e-mail is unknown, so timing does not reveal accounts. */
    private final String dummyHash;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    public UserResponse register(RegisterRequest request) {
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (role == Role.ADMIN) {
            throw new ApiException(ErrorCode.INVALID_ROLE);
        }
        User user = userService.create(request.fullName(), request.email(), request.password(), role);
        return UserResponse.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.email()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            log.warn("Login failed");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPassword());
        if (!passwordMatches || !user.isEnabled()) {
            log.warn("Login failed");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        log.info("Login succeeded: userId={}", user.getId());
        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds(), UserResponse.from(user));
    }
}
