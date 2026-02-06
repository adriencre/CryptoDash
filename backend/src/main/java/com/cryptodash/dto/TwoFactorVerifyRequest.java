package com.cryptodash.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(
        @NotBlank String tempToken,
        @NotBlank String code
) {}
