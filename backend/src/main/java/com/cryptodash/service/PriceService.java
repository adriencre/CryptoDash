package com.cryptodash.service;

import com.cryptodash.dto.PriceTickDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);
    private static final String TOPIC_PRICES = "/topic/prices";

    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String binanceWsUrl;
    private final List<String> symbols;
    private final String topicPrices;

    private volatile WebSocketClient client;
    private final ConcurrentHashMap<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "binance-reconnect");
        t.setDaemon(true);
        return t;
    });

    public PriceService(
            SimpMessageSendingOperations messagingTemplate,
            ObjectMapper objectMapper,
            @Value("${cryptodash.binance.ws-url:wss://stream.binance.com:9443/ws}") String binanceWsUrl,
            @Value("${cryptodash.binance.symbols:btcusdt,ethusdt,bnbusdt,solusdt}") List<String> symbols,
            @Value("${cryptodash.stomp.topic-prices:/topic/prices}") String topicPrices) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.binanceWsUrl = binanceWsUrl;
        this.symbols = symbols;
        this.topicPrices = topicPrices;
    }

    @PostConstruct
    public void start() {
        connect();
        scheduler.scheduleWithFixedDelay(this::ensureConnected, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdown();
        if (client != null) {
            client.close();
        }
    }

    private void ensureConnected() {
        if (client == null || !client.isOpen()) {
            log.info("Binance WebSocket not open, reconnecting...");
            connect();
        }
    }

    private void connect() {
        try {
            String streamParam = symbols.stream()
                    .map(s -> s.toLowerCase() + "@ticker")
                    .reduce((a, b) -> a + "/" + b)
                    .orElse("btcusdt@ticker");
            // Combined stream: wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/ethusdt@ticker
            String baseUrl = binanceWsUrl.replaceFirst("/ws$", "");
            URI uri = URI.create(baseUrl + "/stream?streams=" + streamParam);

            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("Binance WebSocket connected to combined stream");
                }

                @Override
                public void onMessage(String message) {
                    handleBinanceMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("Binance WebSocket closed: {} - {}", code, reason);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Binance WebSocket error", ex);
                }
            };
            client.connect();
        } catch (Exception e) {
            log.error("Failed to connect to Binance", e);
        }
    }

    void handleBinanceMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data.has("e") && "24hrTicker".equals(data.path("e").asText())) {
                PriceTickDto dto = mapToDto(data);
                lastPrices.put(dto.symbol(), dto.lastPrice());
                messagingTemplate.convertAndSend(topicPrices, dto);
            }
        } catch (Exception e) {
            log.debug("Could not parse Binance ticker: {}", e.getMessage());
        }
    }

    private PriceTickDto mapToDto(JsonNode n) {
        String symbol = n.path("s").asText();
        BigDecimal lastPrice = new BigDecimal(n.path("c").asText());
        BigDecimal priceChangePercent = new BigDecimal(n.path("P").asText());
        BigDecimal high24h = new BigDecimal(n.path("h").asText());
        BigDecimal low24h = new BigDecimal(n.path("l").asText());
        BigDecimal volume24h = new BigDecimal(n.path("q").asText());
        long eventTime = n.path("E").asLong();
        return new PriceTickDto(symbol, lastPrice, priceChangePercent, high24h, low24h, volume24h, eventTime);
    }

    /** Dernier prix USDT connu pour un symbole (ex. BTCUSDT). Retourne null si inconnu. */
    public BigDecimal getLastPriceUsdt(String symbol) {
        String key = symbol.toUpperCase().contains("USDT") ? symbol.toUpperCase() : symbol.toUpperCase() + "USDT";
        return lastPrices.get(key);
    }
}
