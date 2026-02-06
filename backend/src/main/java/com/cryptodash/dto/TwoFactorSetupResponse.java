package com.cryptodash.dto;

public record TwoFactorSetupResponse(
        String secret,
        String qrCodeUrl
) {}
