package com.cryptodash.service;

import com.cryptodash.entity.UserFavorite;
import com.cryptodash.repository.UserFavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private UserFavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getFavoriteSymbols_shouldReturnSymbols() {
        UserFavorite fav1 = new UserFavorite(userId, "BTC");
        UserFavorite fav2 = new UserFavorite(userId, "ETH");
        when(favoriteRepository.findByUserIdOrderBySymbol(userId)).thenReturn(List.of(fav1, fav2));

        List<String> symbols = favoriteService.getFavoriteSymbols(userId);

        assertEquals(2, symbols.size());
        assertTrue(symbols.contains("BTC"));
        assertTrue(symbols.contains("ETH"));
    }

    @Test
    void addFavorite_shouldSaveWhenNotExists() {
        String symbol = "BTC";
        when(favoriteRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.empty());

        favoriteService.addFavorite(userId, symbol);

        verify(favoriteRepository, times(1)).save(any(UserFavorite.class));
    }

    @Test
    void addFavorite_shouldNormalizeSymbol() {
        String symbol = "btcusdt";
        when(favoriteRepository.findByUserIdAndSymbol(userId, "BTC")).thenReturn(Optional.empty());

        favoriteService.addFavorite(userId, symbol);

        verify(favoriteRepository, times(1)).save(argThat(fav -> fav.getSymbol().equals("BTC")));
    }

    @Test
    void addFavorite_shouldThrowExceptionForEmptySymbol() {
        assertThrows(IllegalArgumentException.class, () -> favoriteService.addFavorite(userId, ""));
        assertThrows(IllegalArgumentException.class, () -> favoriteService.addFavorite(userId, "USDT"));
    }

    @Test
    void addFavorite_shouldNotSaveWhenExists() {
        String symbol = "BTC";
        when(favoriteRepository.findByUserIdAndSymbol(userId, symbol))
                .thenReturn(Optional.of(new UserFavorite(userId, symbol)));

        favoriteService.addFavorite(userId, symbol);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void removeFavorite_shouldDeleteWhenNotEmpty() {
        String symbol = "BTC";
        favoriteService.removeFavorite(userId, symbol);
        verify(favoriteRepository, times(1)).deleteByUserIdAndSymbol(userId, symbol);
    }

    @Test
    void removeFavorite_shouldNotDeleteWhenEmpty() {
        favoriteService.removeFavorite(userId, "");
        verify(favoriteRepository, never()).deleteByUserIdAndSymbol(any(), any());
    }
}
