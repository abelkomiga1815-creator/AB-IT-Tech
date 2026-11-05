package com.abittech.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void createNormalisesEmailAndStoresOnlyTheHash() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret-Pass1")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.create("  John Doe ", "  John@Example.COM ", "Secret-Pass1", Role.STUDENT);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("John Doe");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed-value").isNotEqualTo("Secret-Pass1");
        assertThat(created.getRole()).isEqualTo(Role.STUDENT);
        assertThat(created.isEnabled()).isTrue();
    }

    @Test
    void createRejectsDuplicateEmailIgnoringCase() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create("John", "JOHN@example.com", "Secret-Pass1", Role.CUSTOMER))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmailLooksUpTheNormalisedAddress() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(java.util.Optional.empty());

        assertThat(userService.findByEmail(" John@Example.com ")).isEmpty();
        verify(userRepository).findByEmail("john@example.com");
    }
}
