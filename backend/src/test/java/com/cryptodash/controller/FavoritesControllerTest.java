package com.cryptodash.controller;

import com.cryptodash.service.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
//@Disabled("Tests désactivés temporairement - problèmes d'authentification avec standaloneSetup")
class FavoritesControllerTest {

    private MockMvc mockMvc;
    private final UUID testUserId = UUID.randomUUID();

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

        mockMvc.perform(get("/api/favorites")
                        .with(authentication(new org.springframework.security.authentication.TestingAuthenticationToken(testUserId, null, "ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("BTC"));
    }

    @Test
    void addFavorite_shouldCallService() throws Exception {
        mockMvc.perform(post("/api/favorites/BTC")
                        .with(authentication(new org.springframework.security.authentication.TestingAuthenticationToken(testUserId, null, "ROLE_USER"))))
                .andExpect(status().isNoContent());

        verify(favoriteService).addFavorite(any(), eq("BTC"));
    }

    @Test
    void removeFavorite_shouldCallService() throws Exception {
        mockMvc.perform(delete("/api/favorites/BTC")
                        .with(authentication(new org.springframework.security.authentication.TestingAuthenticationToken(testUserId, null, "ROLE_USER"))))
                .andExpect(status().isNoContent());

        verify(favoriteService).removeFavorite(any(), eq("BTC"));
    }
}
