package com.cryptodash.controller;

import com.cryptodash.dto.LeaderboardEntryDto;
import com.cryptodash.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final WalletService walletService;

    public LeaderboardController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public List<LeaderboardEntryDto> getLeaderboard() {
        return walletService.getLeaderboard();
    }
}
