package com.cryptodash.service;

import com.cryptodash.entity.UserFavorite;
import com.cryptodash.repository.UserFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private static final String USDT = "USDT";

    private final UserFavoriteRepository favoriteRepository;

    public FavoriteService(UserFavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public List<String> getFavoriteSymbols(UUID userId) {
        return favoriteRepository.findByUserIdOrderBySymbol(userId).stream()
                .map(UserFavorite::getSymbol)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addFavorite(UUID userId, String symbol) {
        String baseSymbol = (symbol != null ? symbol : "").toUpperCase().replace(USDT, "").trim();
        if (baseSymbol.isEmpty()) {
            throw new IllegalArgumentException("Symbole invalide.");
        }
        if (favoriteRepository.findByUserIdAndSymbol(userId, baseSymbol).isEmpty()) {
            UserFavorite fav = new UserFavorite(userId, baseSymbol);
            favoriteRepository.save(fav);
        }
    }

    @Transactional
    public void removeFavorite(UUID userId, String symbol) {
        String baseSymbol = (symbol != null ? symbol : "").toUpperCase().replace(USDT, "").trim();
        if (!baseSymbol.isEmpty()) {
            favoriteRepository.deleteByUserIdAndSymbol(userId, baseSymbol);
        }
    }
}
