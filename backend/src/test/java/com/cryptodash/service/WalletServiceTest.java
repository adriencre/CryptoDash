package com.cryptodash.service;

import com.cryptodash.entity.User;
import com.cryptodash.entity.WalletPosition;
import com.cryptodash.repository.TransactionRepository;
import com.cryptodash.repository.UserRepository;
import com.cryptodash.repository.WalletPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletPositionRepository positionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PriceService priceService;

    @InjectMocks
    private WalletService walletService;

    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
    }

    @Test
    void ensureInitialBalance_ShouldSaveInitialUSDT_WhenNoneExists() {
        when(positionRepository.findByUserIdAndSymbol(userId, "USDT")).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        walletService.ensureInitialBalance(userId);

        verify(positionRepository).save(any(WalletPosition.class));
    }

    @Test
    void buy_ShouldUpdateBalancesAndCreateTransaction() {
        WalletPosition usdtPos = new WalletPosition();
        usdtPos.setSymbol("USDT");
        usdtPos.setAmount(new BigDecimal("1000"));

        when(positionRepository.findByUserIdAndSymbol(userId, "USDT")).thenReturn(Optional.of(usdtPos));
        when(positionRepository.findByUserIdAndSymbol(userId, "BTC")).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        walletService.buy(userId, "BTCUSDT", new BigDecimal("0.1"), new BigDecimal("5000"));

        assertThat(usdtPos.getAmount()).isEqualByComparingTo("500");
        verify(positionRepository, times(2)).save(any(WalletPosition.class));
        verify(transactionRepository).save(any());
    }

    @Test
    void buy_ShouldThrowException_WhenInsufficientFunds() {
        WalletPosition usdtPos = new WalletPosition();
        usdtPos.setSymbol("USDT");
        usdtPos.setAmount(new BigDecimal("100"));

        when(positionRepository.findByUserIdAndSymbol(userId, "USDT")).thenReturn(Optional.of(usdtPos));

        assertThatThrownBy(() -> walletService.buy(userId, "BTCUSDT", new BigDecimal("0.1"), new BigDecimal("5000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solde USDT insuffisant");
    }

    @Test
    void deposit_ShouldUpdateBalance() {
        WalletPosition usdtPos = new WalletPosition();
        usdtPos.setSymbol("USDT");
        usdtPos.setAmount(new BigDecimal("1000"));

        when(positionRepository.findByUserIdAndSymbol(userId, "USDT")).thenReturn(Optional.of(usdtPos));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        walletService.deposit(userId, new BigDecimal("500"));

        assertThat(usdtPos.getAmount()).isEqualByComparingTo("1500");
        verify(positionRepository).save(usdtPos);
    }
}
