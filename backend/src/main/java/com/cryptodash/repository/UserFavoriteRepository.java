package com.cryptodash.repository;

import com.cryptodash.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UUID> {
    List<UserFavorite> findByUserIdOrderBySymbol(UUID userId);
    Optional<UserFavorite> findByUserIdAndSymbol(UUID userId, String symbol);
    void deleteByUserIdAndSymbol(UUID userId, String symbol);
}
