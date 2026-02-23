package com.cryptodash.dto;

import java.math.BigDecimal;

public record LeaderboardEntryDto(
    String accountName,
    String email, // Fallback si accountName est nul
    BigDecimal totalValueUsdt,
    int rank
) {}
