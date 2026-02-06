package com.cryptodash.service;

import com.cryptodash.dto.PriceTickDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private ObjectMapper objectMapper;
    private PriceService priceService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        priceService = new PriceService(
                messagingTemplate,
                objectMapper,
                "wss://stream.binance.com:9443/ws",
                List.of("btcusdt", "ethusdt"),
                "/topic/prices"
        );
    }

    @Test
    void handleBinanceMessage_parsesTickerAndSendsToTopic() {
        String tickerJson = """
                {
                  "e": "24hrTicker",
                  "E": 1691234567890,
                  "s": "BTCUSDT",
                  "p": "123.45",
                  "P": "1.23",
                  "w": "50123.45",
                  "x": "50000.00",
                  "c": "50123.45",
                  "Q": "0.5",
                  "b": "50120.00",
                  "B": "1.2",
                  "a": "50125.00",
                  "A": "2.3",
                  "h": "51000.00",
                  "l": "49000.00",
                  "v": "12345.67",
                  "q": "618000000.00"
                }
                """;

        priceService.handleBinanceMessage(tickerJson);

        ArgumentCaptor<PriceTickDto> captor = ArgumentCaptor.forClass(PriceTickDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/prices"), captor.capture());
        PriceTickDto dto = captor.getValue();
        assertThat(dto.symbol()).isEqualTo("BTCUSDT");
        assertThat(dto.lastPrice()).hasToString("50123.45");
        assertThat(dto.priceChangePercent()).hasToString("1.23");
        assertThat(dto.high24h()).hasToString("51000.00");
        assertThat(dto.low24h()).hasToString("49000.00");
        assertThat(dto.volume24h()).hasToString("618000000.00");
        assertThat(dto.eventTime()).isEqualTo(1691234567890L);
    }

    @Test
    void handleBinanceMessage_combinedStreamUnwrapsData() {
        String combinedJson = """
                {
                  "stream": "btcusdt@ticker",
                  "data": {
                    "e": "24hrTicker",
                    "E": 1691234567890,
                    "s": "BTCUSDT",
                    "c": "40000.00",
                    "P": "-0.50",
                    "h": "41000.00",
                    "l": "39000.00",
                    "q": "1000000.00"
                  }
                }
                """;

        priceService.handleBinanceMessage(combinedJson);

        ArgumentCaptor<PriceTickDto> captor = ArgumentCaptor.forClass(PriceTickDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/prices"), captor.capture());
        PriceTickDto dto = captor.getValue();
        assertThat(dto.symbol()).isEqualTo("BTCUSDT");
        assertThat(dto.lastPrice()).hasToString("40000.00");
        assertThat(dto.priceChangePercent()).hasToString("-0.50");
    }

    @Test
    void handleBinanceMessage_ignoresNonTickerMessages() {
        priceService.handleBinanceMessage("{\"result\":null,\"id\":1}");

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/prices"), any(PriceTickDto.class));
    }
}
