package com.cryptodash.dto;

import java.util.UUID;

/**
 * Réponse du login : soit token complet, soit demande de code 2FA (tempToken).
 */
public record LoginResponse(
        boolean requires2FA,
        String tempToken,
        String token,
        UUID userId,
        String email
) {
    public static LoginResponse with2FARequired(String tempToken, UUID userId, String email) {
        return new LoginResponse(true, tempToken, null, userId, email);
    }

    public static LoginResponse withToken(String token, UUID userId, String email) {
        return new LoginResponse(false, null, token, userId, email);
    }
}
