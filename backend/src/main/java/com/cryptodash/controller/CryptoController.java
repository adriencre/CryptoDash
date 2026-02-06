package com.cryptodash.controller;

import com.cryptodash.dto.KlineDto;
import com.cryptodash.service.CoinGeckoMarketChartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private static final Logger log = LoggerFactory.getLogger(CryptoController.class);
    private final CoinGeckoMarketChartService marketChartService;

    public CryptoController(CoinGeckoMarketChartService marketChartService) {
        this.marketChartService = marketChartService;
    }

    /**
     * Historique des prix via CoinGecko market_chart.
     *
     * @param symbol ex. BTCUSDT ou BTC
     * @param days   1, 7, 14, 30, 90, 180, 365 ou max
     */
    @GetMapping("/{symbol}/market_chart")
    public List<KlineDto> getMarketChart(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "7") String days) {
        String sym = symbol.toUpperCase().contains("USDT") ? symbol.toUpperCase() : symbol.toUpperCase() + "USDT";
        log.info("GET market_chart symbol={} days={}", sym, days);
        List<KlineDto> result = marketChartService.getMarketChart(sym, days);
        log.info("market_chart result size={}", result.size());
        return result;
    }
}
