package com.cryptodash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email
        String email,
        @NotBlank @Size(min = 8, message = "Au moins 8 caractères")
        String password
) {}
