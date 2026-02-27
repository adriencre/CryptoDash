package com.cryptodash.controller;

import com.cryptodash.service.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FavoritesControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoritesController favoritesController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(favoritesController).build();
    }

    @Test
    void getFavorites_shouldReturnList() throws Exception {
        when(favoriteService.getFavoriteSymbols(any())).thenReturn(List.of("BTC", "ETH"));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("BTC"));
    }

    @Test
    void addFavorite_shouldCallService() throws Exception {
        mockMvc.perform(post("/api/favorites/BTC"))
                .andExpect(status().isNoContent());

        verify(favoriteService).addFavorite(any(), eq("BTC"));
    }

    @Test
    void removeFavorite_shouldCallService() throws Exception {
        mockMvc.perform(delete("/api/favorites/BTC"))
                .andExpect(status().isNoContent());

        verify(favoriteService).removeFavorite(any(), eq("BTC"));
    }
}
