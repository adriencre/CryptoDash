package com.cryptodash.repository;

import com.cryptodash.entity.WalletPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletPositionRepository extends JpaRepository<WalletPosition, UUID> {
    List<WalletPosition> findByUserIdOrderBySymbol(UUID userId);
    Optional<WalletPosition> findByUserIdAndSymbol(UUID userId, String symbol);

    @Query("SELECT DISTINCT p.user.id FROM WalletPosition p")
    List<UUID> findDistinctUserIds();
}
