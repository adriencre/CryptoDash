package com.cryptodash.controller;

import com.cryptodash.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {

    private final FavoriteService favoriteService;

    public FavoritesController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<String> getFavorites(@AuthenticationPrincipal UUID userId) {
        return favoriteService.getFavoriteSymbols(userId);
    }

    @PostMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFavorite(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("symbol") String symbol) {
        favoriteService.addFavorite(userId, symbol);
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("symbol") String symbol) {
        favoriteService.removeFavorite(userId, symbol);
    }
}
