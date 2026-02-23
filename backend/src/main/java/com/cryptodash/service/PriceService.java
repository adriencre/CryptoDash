package com.cryptodash.service;

import com.cryptodash.dto.PriceTickDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String binanceWsUrl;
    private final List<String> symbols;
    private final String topicPrices;
    private final String coingeckoBaseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    private volatile WebSocketClient client;
    private final ConcurrentHashMap<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PriceTickDto> lastPriceDtos = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "binance-monitor");
        t.setDaemon(true);
        return t;
    });

    // Mapping complet aligné sur application.yml
    private static final Map<String, String> SYMBOL_MAP = Map.ofEntries(
        Map.entry("BTC", "bitcoin"), Map.entry("ETH", "ethereum"), Map.entry("BNB", "binancecoin"),
        Map.entry("SOL", "solana"), Map.entry("XRP", "ripple"), Map.entry("ADA", "cardano"),
        Map.entry("DOGE", "dogecoin"), Map.entry("AVAX", "avalanche-2"), Map.entry("LINK", "chainlink"),
        Map.entry("DOT", "polkadot"), Map.entry("MATIC", "matic-network"), Map.entry("LTC", "litecoin"),
        Map.entry("ATOM", "cosmos"), Map.entry("UNI", "uniswap"), Map.entry("APT", "aptos"),
        Map.entry("ARB", "arbitrum"), Map.entry("OP", "optimism"), Map.entry("TRX", "tron"),
        Map.entry("SHIB", "shiba-inu"), Map.entry("PEPE", "pepe"), Map.entry("NEAR", "near"),
        Map.entry("SUI", "sui"), Map.entry("SEI", "sei-network"), Map.entry("TIA", "celestia"),
        Map.entry("INJ", "injective-protocol"), Map.entry("FIL", "filecoin"), Map.entry("ICP", "internet-computer"),
        Map.entry("XLM", "stellar"), Map.entry("AAVE", "aave"), Map.entry("MKR", "maker"),
        Map.entry("SAND", "the-sandbox"), Map.entry("MANA", "decentraland"), Map.entry("VET", "vechain"),
        Map.entry("ETC", "ethereum-classic"), Map.entry("STX", "stacks"), Map.entry("IMX", "immutable-x"),
        Map.entry("RNDR", "render-token"), Map.entry("FET", "fetch-ai")
    );

    public PriceService(
            SimpMessageSendingOperations messagingTemplate,
            ObjectMapper objectMapper,
            @Value("${cryptodash.binance.ws-url:wss://stream.binance.com:9443/ws}") String binanceWsUrl,
            @Value("${cryptodash.binance.symbols}") List<String> symbols,
            @Value("${cryptodash.stomp.topic-prices:/topic/prices}") String topicPrices,
            @Value("${cryptodash.coingecko.base-url:https://api.coingecko.com}") String coingeckoBaseUrl) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.binanceWsUrl = binanceWsUrl;
        this.symbols = symbols;
        this.topicPrices = topicPrices;
        this.coingeckoBaseUrl = coingeckoBaseUrl;
    }

    @PostConstruct
    public void start() {
        loadInitialPricesFromCoinGecko();
        connect();
        scheduler.scheduleWithFixedDelay(this::ensureConnected, 10, 10, TimeUnit.SECONDS);
    }

    private void loadInitialPricesFromCoinGecko() {
        try {
            log.info("Loading initial prices for {} symbols from CoinGecko...", symbols.size());
            String ids = symbols.stream()
                .map(s -> s.toUpperCase().replace("USDT", ""))
                .map(SYMBOL_MAP::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(","));

            String url = coingeckoBaseUrl + "/api/v3/coins/markets?vs_currency=usd&ids=" + ids + "&price_change_percentage=24h";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Mozilla/5.0");
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                for (JsonNode n : root) {
                    String sym = n.path("symbol").asText().toUpperCase() + "USDT";
                    PriceTickDto dto = new PriceTickDto(
                        sym,
                        new BigDecimal(n.path("current_price").asText()),
                        new BigDecimal(n.path("price_change_percentage_24h").asText()),
                        new BigDecimal(n.path("high_24h").asText()),
                        new BigDecimal(n.path("low_24h").asText()),
                        new BigDecimal(n.path("total_volume").asText()),
                        Instant.parse(n.path("last_updated").asText()).toEpochMilli()
                    );
                    lastPrices.put(dto.symbol(), dto.lastPrice());
                    lastPriceDtos.put(dto.symbol(), dto);
                }
                log.info("Successfully loaded {} initial prices", lastPriceDtos.size());
            }
        } catch (Exception e) {
            log.warn("CoinGecko initial load failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdown();
        if (client != null) client.close();
    }

    private synchronized void ensureConnected() {
        if (client == null || client.isClosed() || !client.isOpen()) connect();
    }

    private void connect() {
        try {
            String streamParam = symbols.stream().map(String::toLowerCase).map(s -> s.contains("@ticker") ? s : s + "@ticker").collect(Collectors.joining("/"));
            String baseUrl = binanceWsUrl.replaceFirst("/ws$", "");
            URI uri = URI.create(baseUrl + "/stream?streams=" + streamParam);

            client = new WebSocketClient(uri) {
                @Override public void onOpen(ServerHandshake h) { log.info("Binance connected"); }
                @Override public void onMessage(String m) { handleBinanceMessage(m); }
                @Override public void onClose(int c, String r, boolean rem) { log.warn("Binance closed: {}", r); }
                @Override public void onError(Exception e) { log.error("Binance error: {}", e.getMessage()); }
            };
            client.connect();
        } catch (Exception e) {
            log.error("Binance connect failed: {}", e.getMessage());
        }
    }

    public void handleBinanceMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data.has("e") && "24hrTicker".equals(data.path("e").asText())) {
                PriceTickDto dto = mapToDto(data);
                lastPrices.put(dto.symbol(), dto.lastPrice());
                lastPriceDtos.put(dto.symbol(), dto);
                messagingTemplate.convertAndSend(topicPrices, dto);
            }
        } catch (Exception e) { }
    }

    private PriceTickDto mapToDto(JsonNode n) {
        return new PriceTickDto(
            n.path("s").asText().toUpperCase(),
            new BigDecimal(n.path("c").asText()),
            new BigDecimal(n.path("P").asText()),
            new BigDecimal(n.path("h").asText()),
            new BigDecimal(n.path("l").asText()),
            new BigDecimal(n.path("q").asText()),
            n.path("E").asLong()
        );
    }

    public BigDecimal getLastPriceUsdt(String symbol) {
        String key = symbol.toUpperCase().contains("USDT") ? symbol.toUpperCase() : symbol.toUpperCase() + "USDT";
        return lastPrices.get(key);
    }

    public java.util.Collection<PriceTickDto> getCurrentPrices() {
        return lastPriceDtos.values();
    }
}
