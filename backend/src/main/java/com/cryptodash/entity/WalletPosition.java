package com.cryptodash.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Position fictive dans le portefeuille (quantité d'un actif).
 */
@Entity
@Table(name = "wallet_positions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "symbol" })
})
public class WalletPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String symbol; // ex. BTC, ETH, USDT

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal amount = BigDecimal.ZERO;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
