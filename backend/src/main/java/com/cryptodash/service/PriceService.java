package com.cryptodash.service;

import com.cryptodash.dto.PriceTickDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final String coingeckoApiKey;
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
            Map.entry("RNDR", "render-token"), Map.entry("FET", "artificial-superintelligence-alliance"));

    public PriceService(
            SimpMessageSendingOperations messagingTemplate,
            ObjectMapper objectMapper,
            @Value("${cryptodash.binance.ws-url:wss://stream.binance.com:9443/ws}") String binanceWsUrl,
            @Value("${cryptodash.binance.symbols}") List<String> symbols,
            @Value("${cryptodash.stomp.topic-prices:/topic/prices}") String topicPrices,
            @Value("${cryptodash.coingecko.base-url:https://api.coingecko.com}") String coingeckoBaseUrl,
            @Value("${cryptodash.coingecko.api-key:}") String coingeckoApiKey) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.binanceWsUrl = binanceWsUrl;
        this.symbols = symbols;
        this.topicPrices = topicPrices;
        this.coingeckoBaseUrl = coingeckoBaseUrl;
        this.coingeckoApiKey = coingeckoApiKey;
    }

    @PostConstruct
    public void start() {
        // Mock initial prices to ensure stability in tests/dev if APIs are throttled
        symbols.forEach(s -> {
            String sym = s.toUpperCase();
            BigDecimal mockPrice = switch (sym) {
                case "BTCUSDT" -> new BigDecimal("50000");
                case "ETHUSDT" -> new BigDecimal("2500");
                case "BNBUSDT" -> new BigDecimal("300");
                case "SOLUSDT" -> new BigDecimal("100");
                default -> new BigDecimal("10");
            };
            PriceTickDto mockDto = new PriceTickDto(sym, mockPrice, BigDecimal.ZERO, mockPrice, mockPrice,
                    BigDecimal.TEN, Instant.now().toEpochMilli());
            lastPrices.put(sym, mockPrice);
            lastPriceDtos.put(sym, mockDto);
        });

        loadInitialPricesFromCoinGecko();
        connect();

        // Surveillance de la connexion Binance - On laisse plus de temps (30s)
        scheduler.scheduleWithFixedDelay(this::ensureConnected, 30, 30, TimeUnit.SECONDS);

        // FALLBACK: Rafraîchir via CoinGecko toutes les 30 secondes au cas où le WS
        // fail
        scheduler.scheduleWithFixedDelay(this::loadInitialPricesFromCoinGecko, 30, 30, TimeUnit.SECONDS);
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (topicPrices.equals(destination)) {
            log.info("New subscriber to prices, sending current {} prices", lastPriceDtos.size());
            lastPriceDtos.values().forEach(dto -> {
                messagingTemplate.convertAndSend(topicPrices, dto);
            });
        }
    }

    private void loadInitialPricesFromCoinGecko() {
        try {
            log.info("Loading initial prices for {} symbols from CoinGecko...", symbols.size());
            log.info("CoinGecko API key present: {}", coingeckoApiKey != null && !coingeckoApiKey.trim().isEmpty());
            if (coingeckoApiKey != null && !coingeckoApiKey.trim().isEmpty()) {
                log.info("CoinGecko API key starts with: {}", coingeckoApiKey.substring(0, Math.min(10, coingeckoApiKey.length())));
            }
            String ids = symbols.stream()
                    .map(s -> s.toUpperCase().replace("USDT", ""))
                    .map(SYMBOL_MAP::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining(","));

            String url = coingeckoBaseUrl + "/api/v3/coins/markets?vs_currency=usd&ids=" + ids
                    + "&price_change_percentage=24h";
            if (coingeckoApiKey != null && !coingeckoApiKey.trim().isEmpty() && coingeckoApiKey.startsWith("CG-")) {
                url = url + "&x_cg_demo_api_key=" + URLEncoder.encode(coingeckoApiKey, StandardCharsets.UTF_8);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Mozilla/5.0");
            if (coingeckoApiKey != null && !coingeckoApiKey.trim().isEmpty() && !coingeckoApiKey.startsWith("CG-")) {
                headers.set("X-Cg-Pro-Api-Key", coingeckoApiKey);
            }

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                    String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                for (JsonNode n : root) {
                    try {
                        String rawSym = n.path("symbol").asText().toUpperCase();
                        String sym = rawSym + "USDT";

                        BigDecimal currentPrice = n.path("current_price").isNumber()
                                ? n.path("current_price").decimalValue()
                                : BigDecimal.ZERO;
                        BigDecimal change24h = n.path("price_change_percentage_24h").isNumber()
                                ? n.path("price_change_percentage_24h").decimalValue()
                                : BigDecimal.ZERO;
                        BigDecimal high24h = n.path("high_24h").isNumber() ? n.path("high_24h").decimalValue()
                                : BigDecimal.ZERO;
                        BigDecimal low24h = n.path("low_24h").isNumber() ? n.path("low_24h").decimalValue()
                                : BigDecimal.ZERO;
                        BigDecimal volume = n.path("total_volume").isNumber() ? n.path("total_volume").decimalValue()
                                : BigDecimal.ZERO;

                        long lastUpdated = 0;
                        if (n.has("last_updated")) {
                            try {
                                lastUpdated = Instant.parse(n.path("last_updated").asText()).toEpochMilli();
                            } catch (Exception ignored) {
                            }
                        }

                        PriceTickDto dto = new PriceTickDto(
                                sym,
                                currentPrice,
                                change24h,
                                high24h,
                                low24h,
                                volume,
                                lastUpdated);
                        lastPrices.put(dto.symbol(), dto.lastPrice());
                        lastPriceDtos.put(dto.symbol(), dto);
                    } catch (Exception e) {
                        log.warn("Error parsing CoinGecko entry: {}", e.getMessage());
                    }
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
        if (client != null)
            client.close();
    }

    private synchronized void ensureConnected() {
        if (client == null || client.isClosed() || !client.isOpen()) {
            log.info("Binance WS not connected, attempting to connect...");
            connect();
        }
    }

    private void connect() {
        try {
            // Binance multiplex streams URL format:
            // wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/ethusdt@ticker...
            String streamParam = symbols.stream()
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.endsWith("@ticker") ? s : s + "@ticker")
                    .collect(Collectors.joining("/"));

            // On utilise le flux global "All Market Tickers" (!ticker@arr)
            // C'veut dire qu'on reçoit tout en un flux, et on filtre après. Très robuste.
            String url = "wss://stream.binance.com:9443/ws/!ticker@arr";
            URI uri = URI.create(url);

            log.info("Connecting to Binance WebSocket ({} symbols)...", symbols.size());
            log.info("Full WebSocket URI: {}", url);

            Map<String, String> headers = Map.of(
                    "User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            client = new WebSocketClient(uri, headers) {
                @Override
                public void onOpen(ServerHandshake h) {
                    log.info("Binance WebSocket CONNECTED successfully (Global Ticker Stream).");
                }

                @Override
                public void onMessage(String m) {
                    handleBinanceMessage(m);
                }

                @Override
                public void onClose(int c, String r, boolean rem) {
                    log.warn("Binance WebSocket CLOSED. Code: {}, Reason: {}, ByRemote: {}", c, r, rem);
                }

                @Override
                public void onError(Exception e) {
                    log.error("Binance WebSocket ERROR: {} (Type: {})", e.getMessage(), e.getClass().getSimpleName());
                    if (e.getMessage() == null)
                        e.printStackTrace();
                }
            };

            if (url.startsWith("wss")) {
                try {
                    javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                    sslContext.init(null, null, null);
                    client.setSocketFactory(sslContext.getSocketFactory());
                } catch (Exception e) {
                    log.error("Failed to setup SSL context", e);
                }
            }

            client.setConnectionLostTimeout(0); // Désactive le check de perte de connexion interne (plus stable sur
                                                // Mac)
            client.connect();
        } catch (Exception e) {
            log.error("Failed to initiate Binance WS connection: {}", e.getMessage());
        }
    }

    private void sendSubscription() {
        try {
            List<String> params = symbols.stream()
                    .map(s -> s.trim().toLowerCase() + "@ticker")
                    .collect(Collectors.toList());

            Map<String, Object> subRequest = Map.of(
                    "method", "SUBSCRIBE",
                    "params", params,
                    "id", 1);
            String json = objectMapper.writeValueAsString(subRequest);
            client.send(json);
            log.info("Subscription message sent for {} symbols", params.size());
        } catch (Exception e) {
            log.error("Failed to send subscription: {}", e.getMessage());
        }
    }

    private boolean firstPriceLogged = false;

    public void handleBinanceMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // Le flux !ticker@arr envoie un tableau d'objets
            if (root.isArray()) {
                if (!firstPriceLogged) {
                    log.info("First Binance global ticker array received!");
                    firstPriceLogged = true;
                }

                for (JsonNode ticker : root) {
                    processTicker(ticker);
                }
            } else if (root.has("e") && "24hrTicker".equals(root.path("e").asText())) {
                processTicker(root);
            }
        } catch (Exception e) {
            log.warn("Error processing Binance message: {}", e.getMessage());
        }
    }

    private void processTicker(JsonNode ticker) {
        String sym = ticker.path("s").asText().toUpperCase();
        // On ne traite que les symboles qui sont dans notre liste configurée
        if (symbols.stream().anyMatch(s -> s.equalsIgnoreCase(sym))) {
            PriceTickDto dto = mapToDto(ticker);
            lastPrices.put(dto.symbol(), dto.lastPrice());
            lastPriceDtos.put(dto.symbol(), dto);
            messagingTemplate.convertAndSend(topicPrices, dto);
        }
    }

    private PriceTickDto mapToDto(JsonNode n) {
        return new PriceTickDto(
                n.path("s").asText().toUpperCase(),
                new BigDecimal(n.path("c").asText()),
                new BigDecimal(n.path("P").asText()),
                new BigDecimal(n.path("h").asText()),
                new BigDecimal(n.path("l").asText()),
                new BigDecimal(n.path("q").asText()),
                n.path("E").asLong());
    }

    public BigDecimal getLastPriceUsdt(String symbol) {
        String key = symbol.toUpperCase().contains("USDT") ? symbol.toUpperCase() : symbol.toUpperCase() + "USDT";
        return lastPrices.get(key);
    }

    public java.util.Collection<PriceTickDto> getCurrentPrices() {
        return lastPriceDtos.values();
    }
}
