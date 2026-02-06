package com.cryptodash.dto;

import com.cryptodash.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDto(
        String id,
        String type,
        String symbol,
        BigDecimal amount,
        BigDecimal priceUsdt,
        BigDecimal totalUsdt,
        Instant createdAt,
        String counterpartyAccountName
) {
    public static TransactionDto from(Transaction t) {
        return new TransactionDto(
                t.getId().toString(),
                t.getType().name(),
                t.getSymbol(),
                t.getAmount(),
                t.getPriceUsdt(),
                t.getTotalUsdt(),
                t.getCreatedAt(),
                t.getCounterpartyAccountName()
        );
    }
}
