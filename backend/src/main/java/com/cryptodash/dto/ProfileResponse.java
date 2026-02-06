package com.cryptodash.dto;

public record ProfileResponse(
        String email,
        String accountName,
        String firstName,
        String lastName,
        String phone,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String city,
        String country
) {}
