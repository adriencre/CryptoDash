package com.cryptodash.service;

import com.cryptodash.dto.KlineDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CoinGeckoMarketChartService {

    private static final Logger log = LoggerFactory.getLogger(CoinGeckoMarketChartService.class);

    private static final Map<String, String> SYMBOL_TO_COINGECKO_ID = Map.ofEntries(
            Map.entry("BTC", "bitcoin"),
            Map.entry("ETH", "ethereum"),
            Map.entry("BNB", "binancecoin"),
            Map.entry("SOL", "solana"),
            Map.entry("XRP", "ripple"),
            Map.entry("ADA", "cardano"),
            Map.entry("DOGE", "dogecoin"),
            Map.entry("AVAX", "avalanche-2"),
            Map.entry("LINK", "chainlink"),
            Map.entry("DOT", "polkadot"),
            Map.entry("MATIC", "matic-network"),
            Map.entry("LTC", "litecoin"),
            Map.entry("ATOM", "cosmos"),
            Map.entry("UNI", "uniswap"),
            Map.entry("APT", "aptos"),
            Map.entry("ARB", "arbitrum"),
            Map.entry("OP", "optimism"),
            Map.entry("TRX", "tron"),
            Map.entry("SHIB", "shiba-inu"),
            Map.entry("PEPE", "pepe"),
            Map.entry("NEAR", "near"),
            Map.entry("SUI", "sui"),
            Map.entry("SEI", "sei-network"),
            Map.entry("TIA", "celestia"),
            Map.entry("INJ", "injective-protocol"),
            Map.entry("FIL", "filecoin"),
            Map.entry("ICP", "internet-computer"),
            Map.entry("XLM", "stellar"),
            Map.entry("AAVE", "aave"),
            Map.entry("MKR", "maker"),
            Map.entry("SAND", "the-sandbox"),
            Map.entry("MANA", "decentraland"),
            Map.entry("VET", "vechain"),
            Map.entry("ETC", "ethereum-classic"),
            Map.entry("STX", "stacks"),
            Map.entry("IMX", "immutable-x"),
            Map.entry("RNDR", "render-token"),
            Map.entry("FET", "fetch-ai"));

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String coingeckoBaseUrl;
    private final String coingeckoApiKey;

    public CoinGeckoMarketChartService(
            RestTemplate restTemplate,
            @Value("${cryptodash.coingecko.base-url:https://api.coingecko.com}") String coingeckoBaseUrl,
            @Value("${cryptodash.coingecko.api-key:}") String coingeckoApiKey) {
        this.restTemplate = restTemplate;
        this.coingeckoBaseUrl = coingeckoBaseUrl;
        this.coingeckoApiKey = coingeckoApiKey;
    }

    /**
     * Récupère l'historique des prix depuis CoinGecko market_chart.
     *
     * @param symbol ex. BTCUSDT ou BTC
     * @param days   1, 7, 14, 30, 90, 180, 365 ou "max"
     */
    public List<KlineDto> getMarketChart(String symbol, String days) {
        String baseSymbol = symbol.toUpperCase().replace("USDT", "");
        String coinId = SYMBOL_TO_COINGECKO_ID.get(baseSymbol);
        if (coinId == null) {
            log.warn("Unknown symbol for CoinGecko: {}", baseSymbol);
            return List.of();
        }
        String url = coingeckoBaseUrl + "/api/v3/coins/" + coinId + "/market_chart?vs_currency=usd&days=" + days;
        if (coingeckoApiKey != null && !coingeckoApiKey.trim().isEmpty() && coingeckoApiKey.startsWith("CG-")) {
            url = url + "&x_cg_demo_api_key=" + URLEncoder.encode(coingeckoApiKey, StandardCharsets.UTF_8);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            if (coingeckoApiKey != null && !coingeckoApiKey.trim().isEmpty() && !coingeckoApiKey.startsWith("CG-")) {
                headers.set("X-Cg-Pro-Api-Key", coingeckoApiKey);
            }
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                    String.class);
            String json = response.getBody();
            if (json == null) {
                log.warn("CoinGecko market_chart empty body for {} days={}", symbol, days);
                return List.of();
            }
            JsonNode root = objectMapper.readTree(json);
            JsonNode prices = root.get("prices");
            if (prices == null || !prices.isArray()) {
                log.warn("CoinGecko market_chart no prices array for {} days={}", symbol, days);
                return List.of();
            }
            List<KlineDto> result = StreamSupport.stream(prices.spliterator(), false)
                    .filter(JsonNode::isArray)
                    .filter(node -> node.size() >= 2)
                    .map(node -> {
                        long openTime = node.get(0).asLong();
                        BigDecimal price = BigDecimal.valueOf(node.get(1).asDouble());
                        return new KlineDto(openTime, price, price, price, price, BigDecimal.ZERO);
                    })
                    .collect(Collectors.toList());
            log.info("CoinGecko market_chart {} days={} -> {} points", symbol, days, result.size());
            return result;
        } catch (RestClientResponseException e) {
            log.warn("CoinGecko API error for {} days={}: status={} body={}", symbol, days, e.getStatusCode(),
                    e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch CoinGecko market_chart for {} days={}: {}", symbol, days, e.getMessage());
            return List.of();
        }
    }
}
