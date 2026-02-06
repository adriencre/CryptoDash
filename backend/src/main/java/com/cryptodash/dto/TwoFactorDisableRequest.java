package com.cryptodash.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorDisableRequest(
        @NotBlank String password,
        @NotBlank String code
) {}
