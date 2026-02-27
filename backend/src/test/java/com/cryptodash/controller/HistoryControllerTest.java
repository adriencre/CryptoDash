package com.cryptodash.controller;

import com.cryptodash.entity.Transaction;
import com.cryptodash.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
//@Disabled("Tests désactivés temporairement - problèmes d'authentification avec standaloneSetup")
class HistoryControllerTest {

    private MockMvc mockMvc;
    private final UUID testUserId = UUID.randomUUID();

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private HistoryController historyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(historyController).build();
    }

    @Test
    void getHistory_shouldReturnList() throws Exception {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setSymbol("BTC");
        tx.setAmount(new BigDecimal("1.0"));
        tx.setType(Transaction.Type.BUY);
        tx.setPriceUsdt(new BigDecimal("50000"));
        tx.setTotalUsdt(new BigDecimal("50000"));
        tx.setCreatedAt(Instant.now());

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(List.of(tx));

        mockMvc.perform(get("/api/history").param("size", "10")
                        .with(authentication(new org.springframework.security.authentication.TestingAuthenticationToken(testUserId, null, "ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("BTC"));
    }
}
