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
import static org.mockito.ArgumentMatchers.eq;
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
                "/topic/prices",
                "https://api.coingecko.com",
                null  // Pas de clé API dans les tests
        );
    }

    @Test
    void handleBinanceMessage_parsesTickerAndSendsToTopic() {
        String tickerJson = """
                {
                  "e": "24hrTicker",
                  "E": 1691234567890,
                  "s": "BTCUSDT",
                  "c": "50123.45",
                  "P": "1.23",
                  "h": "51000.00",
                  "l": "49000.00",
                  "q": "618000000.00"
                }
                """;

        priceService.handleBinanceMessage(tickerJson);

        ArgumentCaptor<PriceTickDto> captor = ArgumentCaptor.forClass(PriceTickDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/prices"), captor.capture());
        PriceTickDto dto = captor.getValue();
        assertThat(dto.symbol()).isEqualTo("BTCUSDT");
        assertThat(dto.lastPrice()).hasToString("50123.45");
    }
}
