package com.cryptodash.controller;

import com.cryptodash.dto.BuySellRequest;
import com.cryptodash.dto.DepositRequest;
import com.cryptodash.dto.PnlSummaryDto;
import com.cryptodash.dto.PerformancePointDto;
import com.cryptodash.dto.SendCryptoRequest;
import com.cryptodash.dto.WalletSummaryDto;
import com.cryptodash.service.PortfolioSnapshotService;
import com.cryptodash.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final PortfolioSnapshotService portfolioSnapshotService;

    public WalletController(WalletService walletService, PortfolioSnapshotService portfolioSnapshotService) {
        this.walletService = walletService;
        this.portfolioSnapshotService = portfolioSnapshotService;
    }

    @GetMapping
    public WalletSummaryDto getWallet(@AuthenticationPrincipal UUID userId) {
        walletService.ensureInitialBalance(userId);
        return walletService.getWallet(userId);
    }

    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.OK)
    public void buy(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody BuySellRequest request,
            @RequestParam BigDecimal priceUsdt) {
        String symbol = request.symbol().toUpperCase().contains("USDT") ? request.symbol().toUpperCase()
                : request.symbol().toUpperCase() + "USDT";
        walletService.buy(userId, symbol, request.amount(), priceUsdt);
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.OK)
    public void sell(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody BuySellRequest request,
            @RequestParam BigDecimal priceUsdt) {
        String symbol = request.symbol().toUpperCase().contains("USDT") ? request.symbol().toUpperCase()
                : request.symbol().toUpperCase() + "USDT";
        walletService.sell(userId, symbol, request.amount(), priceUsdt);
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    public void send(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SendCryptoRequest request) {
        String baseSymbol = request.symbol().toUpperCase().replace("USDT", "");
        walletService.sendCrypto(userId, request.recipientIdentifier(), baseSymbol, request.amount());
    }

    @GetMapping("/performance")
    public List<PerformancePointDto> getPerformance(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "7d") String period) {
        Instant to = Instant.now();
        Instant from = switch (period != null ? period.toLowerCase() : "7d") {
            case "30d" -> to.minus(30, ChronoUnit.DAYS);
            case "90d" -> to.minus(90, ChronoUnit.DAYS);
            default -> to.minus(7, ChronoUnit.DAYS);
        };
        portfolioSnapshotService.recordSnapshot(userId);
        return portfolioSnapshotService.getPerformance(userId, from, to);
    }

    @GetMapping("/pnl")
    public PnlSummaryDto getPnl(@AuthenticationPrincipal UUID userId) {
        return walletService.computePnl(userId);
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.OK)
    public void deposit(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DepositRequest request) {
        walletService.deposit(userId, request.amount());
    }
}
