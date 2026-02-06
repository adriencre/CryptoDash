package com.cryptodash.dto;

import java.math.BigDecimal;

/**
 * DTO exposé aux clients pour le streaming des prix (aligné sur l'UI).
 */
public record PriceTickDto(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal priceChangePercent,
        BigDecimal high24h,
        BigDecimal low24h,
        BigDecimal volume24h,
        Long eventTime
) {}
