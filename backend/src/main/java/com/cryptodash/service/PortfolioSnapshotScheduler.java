package com.cryptodash.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cryptodash.snapshot.enabled", havingValue = "true", matchIfMissing = true)
public class PortfolioSnapshotScheduler {

    private final PortfolioSnapshotService portfolioSnapshotService;

    public PortfolioSnapshotScheduler(PortfolioSnapshotService portfolioSnapshotService) {
        this.portfolioSnapshotService = portfolioSnapshotService;
    }

    @Scheduled(cron = "${cryptodash.snapshot.cron:0 0 * * * *}")
    public void recordSnapshots() {
        portfolioSnapshotService.recordSnapshotsForAllUsers();
    }
}
