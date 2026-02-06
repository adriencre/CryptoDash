package com.cryptodash.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Point ou bougie pour l'historique des prix (CoinGecko market_chart : open=high=low=close).
 */
public record KlineDto(
        long openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {
    public Instant openTimeInstant() {
        return Instant.ofEpochMilli(openTime);
    }
}
