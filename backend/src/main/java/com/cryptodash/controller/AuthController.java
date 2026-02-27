package com.cryptodash.controller;

import com.cryptodash.dto.*;
import com.cryptodash.service.AuthService;
import com.cryptodash.service.TwoFactorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TwoFactorService twoFactorService;

    public AuthController(AuthService authService, TwoFactorService twoFactorService) {
        this.authService = authService;
        this.twoFactorService = twoFactorService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/2fa/verify")
    public AuthResponse verify2FA(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return authService.verify2FA(request.tempToken(), request.code());
    }

    @GetMapping("/2fa/status")
    public TwoFactorStatusResponse get2FAStatus(@AuthenticationPrincipal UUID userId) {
        return authService.get2FAStatus(userId);
    }

    @PostMapping("/2fa/setup")
    public TwoFactorSetupResponse setup2FA(@AuthenticationPrincipal UUID userId) {
        TwoFactorService.TwoFactorSetupResult result = twoFactorService.setupTwoFactor(userId);
        return new TwoFactorSetupResponse(result.secret(), result.qrCodeUrl());
    }

    @PostMapping("/2fa/enable")
    @ResponseStatus(HttpStatus.OK)
    public List<String> enable2FA(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TwoFactorEnableRequest request) {
        return twoFactorService.enableTwoFactor(userId, request.code());
    }

    @PostMapping("/2fa/disable")
    @ResponseStatus(HttpStatus.OK)
    public void disable2FA(@AuthenticationPrincipal UUID userId, @Valid @RequestBody TwoFactorDisableRequest request) {
        twoFactorService.disableTwoFactor(userId, request.password(), request.code());
    }

    @PostMapping("/2fa/backup-codes/regenerate")
    public List<String> regenerateBackupCodes(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        return twoFactorService.regenerateBackupCodes(userId, request.password(), request.code());
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(@AuthenticationPrincipal UUID userId) {
        return authService.getProfile(userId);
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(userId, request);
    }
}
