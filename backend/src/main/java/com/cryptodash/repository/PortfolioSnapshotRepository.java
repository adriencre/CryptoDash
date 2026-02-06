package com.cryptodash.repository;

import com.cryptodash.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {
    List<PortfolioSnapshot> findByUserIdAndSnapshotAtBetweenOrderBySnapshotAtAsc(UUID userId, Instant from, Instant to);
}
