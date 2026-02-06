package com.cryptodash.service;

import com.cryptodash.dto.PerformancePointDto;
import com.cryptodash.entity.PortfolioSnapshot;
import com.cryptodash.repository.PortfolioSnapshotRepository;
import com.cryptodash.repository.WalletPositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PortfolioSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSnapshotService.class);
    private static final int MAX_POINTS = 100;

    private final WalletService walletService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final WalletPositionRepository positionRepository;

    public PortfolioSnapshotService(WalletService walletService,
                                    PortfolioSnapshotRepository snapshotRepository,
                                    WalletPositionRepository positionRepository) {
        this.walletService = walletService;
        this.snapshotRepository = snapshotRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Enregistre un snapshot de la valeur actuelle du portefeuille pour l'utilisateur.
     */
    public void recordSnapshot(UUID userId) {
        BigDecimal total = walletService.computeCurrentTotalUsdt(userId);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(userId, Instant.now(), total);
        snapshotRepository.save(snapshot);
    }

    /**
     * Retourne les points de performance dans la plage [from, to], éventuellement sous-échantillonnés (max 100 points).
     */
    public List<PerformancePointDto> getPerformance(UUID userId, Instant from, Instant to) {
        List<PortfolioSnapshot> snapshots = snapshotRepository.findByUserIdAndSnapshotAtBetweenOrderBySnapshotAtAsc(userId, from, to);
        if (snapshots.isEmpty()) {
            return List.of();
        }
        List<PerformancePointDto> points = new ArrayList<>();
        if (snapshots.size() <= MAX_POINTS) {
            for (PortfolioSnapshot s : snapshots) {
                points.add(new PerformancePointDto(s.getSnapshotAt(), s.getTotalUsdt()));
            }
        } else {
            int step = snapshots.size() / MAX_POINTS;
            if (step < 1) step = 1;
            for (int i = 0; i < snapshots.size(); i += step) {
                PortfolioSnapshot s = snapshots.get(i);
                points.add(new PerformancePointDto(s.getSnapshotAt(), s.getTotalUsdt()));
            }
            PortfolioSnapshot last = snapshots.get(snapshots.size() - 1);
            if (!points.get(points.size() - 1).timestamp().equals(last.getSnapshotAt())) {
                points.add(new PerformancePointDto(last.getSnapshotAt(), last.getTotalUsdt()));
            }
        }
        return points;
    }

    /**
     * Enregistre un snapshot pour chaque utilisateur ayant au moins une position.
     */
    public void recordSnapshotsForAllUsers() {
        List<UUID> userIds = positionRepository.findDistinctUserIds();
        for (UUID userId : userIds) {
            try {
                recordSnapshot(userId);
            } catch (Exception e) {
                log.warn("Failed to record snapshot for user {}: {}", userId, e.getMessage());
            }
        }
    }
}
