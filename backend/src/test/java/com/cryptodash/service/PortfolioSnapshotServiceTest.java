package com.cryptodash.service;

import com.cryptodash.dto.PerformancePointDto;
import com.cryptodash.entity.PortfolioSnapshot;
import com.cryptodash.repository.PortfolioSnapshotRepository;
import com.cryptodash.repository.WalletPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioSnapshotServiceTest {

    @Mock
    private WalletService walletService;
    @Mock
    private PortfolioSnapshotRepository snapshotRepository;
    @Mock
    private WalletPositionRepository positionRepository;

    @InjectMocks
    private PortfolioSnapshotService snapshotService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void recordSnapshot_shouldSaveSnapshot() {
        when(walletService.computeCurrentTotalUsdt(userId)).thenReturn(new BigDecimal("1000.0"));

        snapshotService.recordSnapshot(userId);

        verify(snapshotRepository).save(any(PortfolioSnapshot.class));
    }

    @Test
    void getPerformance_shouldReturnAllPointsWhenFew() {
        Instant now = Instant.now();
        List<PortfolioSnapshot> snapshots = List.of(
                new PortfolioSnapshot(userId, now.minusSeconds(60), new BigDecimal("100")),
                new PortfolioSnapshot(userId, now, new BigDecimal("110")));
        when(snapshotRepository.findByUserIdAndSnapshotAtBetweenOrderBySnapshotAtAsc(eq(userId), any(), any()))
                .thenReturn(snapshots);

        List<PerformancePointDto> result = snapshotService.getPerformance(userId, now.minusSeconds(120), now);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("100"), result.get(0).totalUsdt());
    }

    @Test
    void getPerformance_shouldDownsampleWhenMany() {
        Instant now = Instant.now();
        List<PortfolioSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            snapshots.add(new PortfolioSnapshot(userId, now.plusSeconds(i), new BigDecimal(i)));
        }
        when(snapshotRepository.findByUserIdAndSnapshotAtBetweenOrderBySnapshotAtAsc(eq(userId), any(), any()))
                .thenReturn(snapshots);

        List<PerformancePointDto> result = snapshotService.getPerformance(userId, now, now.plusSeconds(200));

        // It should be around 100-101 points due to sampling logic
        assertTrue(result.size() <= 101);
        assertEquals(new BigDecimal("0"), result.get(0).totalUsdt());
        assertEquals(new BigDecimal("199"), result.get(result.size() - 1).totalUsdt());
    }

    @Test
    void recordSnapshotsForAllUsers_shouldRecordForEachUser() {
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(positionRepository.findDistinctUserIds()).thenReturn(userIds);
        when(walletService.computeCurrentTotalUsdt(any())).thenReturn(BigDecimal.ZERO);

        snapshotService.recordSnapshotsForAllUsers();

        verify(snapshotRepository, times(2)).save(any(PortfolioSnapshot.class));
    }
}
