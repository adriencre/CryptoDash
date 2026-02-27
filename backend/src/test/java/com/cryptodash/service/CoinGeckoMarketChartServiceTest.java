package com.cryptodash.service;

import com.cryptodash.dto.KlineDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoinGeckoMarketChartServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private CoinGeckoMarketChartService marketChartService;

    @BeforeEach
    void setUp() {
        marketChartService = new CoinGeckoMarketChartService(restTemplate, "https://api.coingecko.com", null);
    }

    @Test
    void getMarketChart_shouldReturnList() throws Exception {
        String jsonResponse = "{\"prices\":[[1700000000000, 50000.0], [1700000060000, 50100.0]]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(jsonResponse));

        List<KlineDto> result = marketChartService.getMarketChart("BTCUSDT", "7");

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("50000.0"), result.get(0).open());
        assertEquals(1700000000000L, result.get(0).openTime());
    }

    @Test
    void getMarketChart_shouldReturnEmptyOnUnknownSymbol() {
        List<KlineDto> result = marketChartService.getMarketChart("UNKNOWN", "7");
        assertTrue(result.isEmpty());
    }

    @Test
    void getMarketChart_shouldReturnEmptyOnError() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("API Down"));

        List<KlineDto> result = marketChartService.getMarketChart("BTC", "1");
        assertTrue(result.isEmpty());
    }
}
