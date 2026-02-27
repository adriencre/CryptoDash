package com.cryptodash.integration;

import com.cryptodash.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev-h2")
public class BackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private UUID userId;
    private final String email = "test@example.com";
    private final String password = "password123";

    @BeforeEach
    void setUp() throws Exception {
        // 1. Register
        RegisterRequest registerRequest = new RegisterRequest(email, password);
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);
        this.authToken = authResponse.token();
        this.userId = authResponse.userId();
    }

    @Test
    void fullBackendIntegrationTest() throws Exception {
        // 2. Login
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        assertThat(loginResponse.token()).isNotNull();

        // 3. Wallet Initial State
        MvcResult walletResult = mockMvc.perform(get("/api/wallet")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();
        WalletSummaryDto walletSummary = objectMapper.readValue(walletResult.getResponse().getContentAsString(), WalletSummaryDto.class);
        assertThat(walletSummary.positions()).hasSize(1);
        assertThat(walletSummary.positions().get(0).symbol()).isEqualTo("USDT");

        // 4. Deposit
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("1000.00"));
        mockMvc.perform(post("/api/wallet/deposit")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // 5. Buy BTC
        BuySellRequest buyRequest = new BuySellRequest("BTC", new BigDecimal("0.1"));
        mockMvc.perform(post("/api/wallet/buy")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest))
                        .param("priceUsdt", "50000"))
                .andExpect(status().isOk());

        // 6. Favorites
        mockMvc.perform(post("/api/favorites/BTC")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        MvcResult favoritesResult = mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();
        List<String> favorites = objectMapper.readValue(favoritesResult.getResponse().getContentAsString(), new TypeReference<List<String>>() {});
        assertThat(favorites).containsExactly("BTC");

        // 7. Transaction History
        MvcResult historyResult = mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();
        List<TransactionDto> history = objectMapper.readValue(historyResult.getResponse().getContentAsString(), new TypeReference<List<TransactionDto>>() {});
        
        // Should have 2 transactions: DEPOSIT and BUY
        assertThat(history).hasSize(2);
        assertThat(history.get(0).type()).isEqualTo("BUY");
        assertThat(history.get(1).type()).isEqualTo("DEPOSIT");

        // 8. Remove Favorite
        mockMvc.perform(delete("/api/favorites/BTC")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        favoritesResult = mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();
        favorites = objectMapper.readValue(favoritesResult.getResponse().getContentAsString(), new TypeReference<List<String>>() {});
        assertThat(favorites).isEmpty();
    }
}
