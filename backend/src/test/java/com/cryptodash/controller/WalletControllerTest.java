package com.cryptodash.controller;

import com.cryptodash.dto.WalletSummaryDto;
import com.cryptodash.service.PortfolioSnapshotService;
import com.cryptodash.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @MockBean
    private PortfolioSnapshotService portfolioSnapshotService;

    @MockBean
    private com.cryptodash.security.JwtService jwtService;

    @MockBean
    private com.cryptodash.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getWallet_ShouldReturn200() throws Exception {
        when(walletService.getWallet(any())).thenReturn(new WalletSummaryDto(Collections.emptyList()));

        mockMvc.perform(get("/api/wallet")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
