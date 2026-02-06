package com.cryptodash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Email @Size(max = 255)
        String email,
        @Size(max = 50)
        String accountName,
        @Size(max = 100)
        String firstName,
        @Size(max = 100)
        String lastName,
        @Size(max = 30)
        String phone,
        @Size(max = 255)
        String addressLine1,
        @Size(max = 255)
        String addressLine2,
        @Size(max = 20)
        String postalCode,
        @Size(max = 100)
        String city,
        @Size(max = 100)
        String country
) {}
