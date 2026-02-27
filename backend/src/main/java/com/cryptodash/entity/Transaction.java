package com.cryptodash.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_user_created", columnList = "user_id, created_at")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal priceUsdt;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal totalUsdt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Pour SEND/RECEIVE : l'autre utilisateur. */
    @Column(name = "counterparty_user_id")
    private UUID counterpartyUserId;

    /** Pour SEND/RECEIVE : nom de compte de l'autre partie (affichage). */
    @Column(name = "counterparty_account_name", length = 50)
    private String counterpartyAccountName;

    public enum Type {
        BUY, SELL, SEND, RECEIVE, DEPOSIT
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPriceUsdt() {
        return priceUsdt;
    }

    public void setPriceUsdt(BigDecimal priceUsdt) {
        this.priceUsdt = priceUsdt;
    }

    public BigDecimal getTotalUsdt() {
        return totalUsdt;
    }

    public void setTotalUsdt(BigDecimal totalUsdt) {
        this.totalUsdt = totalUsdt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCounterpartyUserId() {
        return counterpartyUserId;
    }

    public void setCounterpartyUserId(UUID counterpartyUserId) {
        this.counterpartyUserId = counterpartyUserId;
    }

    public String getCounterpartyAccountName() {
        return counterpartyAccountName;
    }

    public void setCounterpartyAccountName(String counterpartyAccountName) {
        this.counterpartyAccountName = counterpartyAccountName;
    }
}
