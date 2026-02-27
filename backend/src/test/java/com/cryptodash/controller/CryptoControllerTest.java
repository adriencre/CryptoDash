package com.cryptodash.controller;

import com.cryptodash.dto.KlineDto;
import com.cryptodash.dto.PriceTickDto;
import com.cryptodash.service.CoinGeckoMarketChartService;
import com.cryptodash.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CryptoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CoinGeckoMarketChartService marketChartService;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private CryptoController cryptoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cryptoController).build();
    }

    @Test
    void getCurrentPrices_shouldReturnList() throws Exception {
        PriceTickDto tick = new PriceTickDto("BTCUSDT", new BigDecimal("50000"), BigDecimal.ZERO,
                new BigDecimal("51000"), new BigDecimal("49000"), new BigDecimal("1000"), System.currentTimeMillis());
        when(priceService.getCurrentPrices()).thenReturn(List.of(tick));

        mockMvc.perform(get("/api/crypto/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("BTCUSDT"));
    }

    @Test
    void getMarketChart_shouldReturnList() throws Exception {
        KlineDto kline = new KlineDto(System.currentTimeMillis(), new BigDecimal("50000"), new BigDecimal("51000"),
                new BigDecimal("49000"), new BigDecimal("50500"), BigDecimal.ZERO);
        when(marketChartService.getMarketChart(anyString(), anyString())).thenReturn(List.of(kline));

        mockMvc.perform(get("/api/crypto/BTC/market_chart").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].open").value(50000));
    }
}
