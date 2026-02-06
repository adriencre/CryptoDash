package com.cryptodash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Demande d'envoi de crypto vers un autre compte (email ou nom de compte). */
public record SendCryptoRequest(
        @NotBlank(message = "Indiquez l'email ou le nom de compte du destinataire.")
        String recipientIdentifier,
        @NotBlank(message = "Indiquez l'actif à envoyer (ex: BTC, ETH).")
        String symbol,
        @NotNull @DecimalMin("0.00000001")
        BigDecimal amount
) {}
