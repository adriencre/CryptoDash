package com.cryptodash.service;

import com.cryptodash.dto.AuthResponse;
import com.cryptodash.dto.LoginRequest;
import com.cryptodash.dto.LoginResponse;
import com.cryptodash.dto.RegisterRequest;
import com.cryptodash.entity.User;
import com.cryptodash.repository.UserRepository;
import com.cryptodash.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private TwoFactorService twoFactorService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String password = "password123";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail(email);
        testUser.setPasswordHash("hashedPassword");
    }

    @Test
    void register_ShouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest(email, password);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(userId, email)).thenReturn("test-token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.email()).isEqualTo(email);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowExceptionIfEmailExists() {
        RegisterRequest request = new RegisterRequest(email, password);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Un compte existe déjà");
    }

    @Test
    void login_ShouldReturnToken_When2FADisabled() {
        LoginRequest request = new LoginRequest(email, password);
        testUser.setTwoFactorEnabled(false);
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(userId, email)).thenReturn("test-token");

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.requires2FA()).isFalse();
    }

    @Test
    void login_ShouldReturnTempToken_When2FAEnabled() {
        LoginRequest request = new LoginRequest(email, password);
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateTempToken(userId)).thenReturn("temp-token");

        LoginResponse response = authService.login(request);

        assertThat(response.tempToken()).isEqualTo("temp-token");
        assertThat(response.requires2FA()).isTrue();
    }

    @Test
    void login_ShouldThrowException_WhenInvalidCredentials() {
        LoginRequest request = new LoginRequest(email, "wrongPassword");
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
