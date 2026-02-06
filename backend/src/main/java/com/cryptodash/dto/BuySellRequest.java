package com.cryptodash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BuySellRequest(
        @NotBlank String symbol,  // ex. BTC, ETH
        @NotNull @DecimalMin("0.00000001") BigDecimal amount
) {}
