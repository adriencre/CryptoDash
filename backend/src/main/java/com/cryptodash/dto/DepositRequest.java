package com.cryptodash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull(message = "Le montant est requis.") @DecimalMin(value = "1.0", inclusive = true, message = "Le montant minimum est de 1 USDT.") BigDecimal amount) {
}
