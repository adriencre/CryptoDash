package com.cryptodash.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio_snapshots", indexes = {
        @Index(name = "idx_portfolio_snapshots_user_snapshot", columnList = "user_id, snapshot_at")
})
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "total_usdt", nullable = false, precision = 24, scale = 8)
    private BigDecimal totalUsdt;

    public PortfolioSnapshot() {}

    public PortfolioSnapshot(UUID userId, Instant snapshotAt, BigDecimal totalUsdt) {
        this.userId = userId;
        this.snapshotAt = snapshotAt;
        this.totalUsdt = totalUsdt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getSnapshotAt() { return snapshotAt; }
    public void setSnapshotAt(Instant snapshotAt) { this.snapshotAt = snapshotAt; }
    public BigDecimal getTotalUsdt() { return totalUsdt; }
    public void setTotalUsdt(BigDecimal totalUsdt) { this.totalUsdt = totalUsdt; }
}
