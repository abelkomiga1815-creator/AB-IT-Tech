package com.abittech.platform.user.service;

import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.exception.ResourceNotFoundException;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account use cases. The only place where e-mail normalisation and password hashing happen.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(String fullName, String email, String rawPassword, Role role) {
        String normalizedEmail = Strings.normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = new User(fullName.trim(), normalizedEmail, passwordEncoder.encode(rawPassword), role);
        User saved = userRepository.save(user);
        log.info("Account created: userId={}, role={}", saved.getId(), saved.getRole());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(Strings.normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public User updateFullName(UUID id, String fullName) {
        User user = getById(id);
        user.setFullName(fullName.trim());
        log.info("Account name updated: userId={}", id);
        return user;
    }
}
