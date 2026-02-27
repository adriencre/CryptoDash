package com.cryptodash.service;

import com.cryptodash.entity.BackupCode;
import com.cryptodash.entity.User;
import com.cryptodash.repository.BackupCodeRepository;
import com.cryptodash.repository.UserRepository;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BackupCodeRepository backupCodeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TwoFactorService twoFactorService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setTwoFactorEnabled(false);
    }

    @Test
    void setupTwoFactor_shouldGenerateSecret() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        TwoFactorService.TwoFactorSetupResult result = twoFactorService.setupTwoFactor(userId);

        assertNotNull(result.secret());
        assertNotNull(result.qrCodeUrl());
        assertTrue(result.qrCodeUrl().contains(result.secret()));
        assertEquals(result.secret(), user.getTwoFactorSecret());
        verify(userRepository).save(user);
    }

    @Test
    void setupTwoFactor_shouldThrowIfAlreadyEnabled() {
        user.setTwoFactorEnabled(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> twoFactorService.setupTwoFactor(userId));
    }

    @Test
    void enableTwoFactor_shouldVerifyAndEnable() throws Exception {
        String secret = "JBSWY3DPEHPK3PXP"; // Base32 for "Hello!"
        user.setTwoFactorSecret(secret);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Generate valid code
        String code = generateCurrentCode(secret);

        List<String> backupCodes = twoFactorService.enableTwoFactor(userId, code);

        assertTrue(user.isTwoFactorEnabled());
        assertEquals(12, backupCodes.size());
        verify(userRepository).save(user);
        verify(backupCodeRepository, times(12)).save(any(BackupCode.class));
    }

    @Test
    void enableTwoFactor_shouldThrowIfCodeInvalid() {
        user.setTwoFactorSecret("JBSWY3DPEHPK3PXP");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> twoFactorService.enableTwoFactor(userId, "000000"));
    }

    @Test
    void disableTwoFactor_shouldVerifyAndDisable() throws Exception {
        String secret = "JBSWY3DPEHPK3PXP";
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret(secret);
        user.setPasswordHash("hashedPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);

        String code = generateCurrentCode(secret);

        twoFactorService.disableTwoFactor(userId, "password", code);

        assertFalse(user.isTwoFactorEnabled());
        assertNull(user.getTwoFactorSecret());
        verify(backupCodeRepository).deleteByUserId(userId);
    }

    @Test
    void verifyTwoFactorCode_shouldReturnTrueForValidTotp() throws Exception {
        String secret = "JBSWY3DPEHPK3PXP";
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret(secret);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String code = generateCurrentCode(secret);

        assertTrue(twoFactorService.verifyTwoFactorCode(userId, code));
    }

    @Test
    void verifyTwoFactorCode_shouldReturnTrueForBackupCode() {
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret("SECRET");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Mock consumeBackupCode (which is protected, but we test through public
        // method)
        // Since it's protected, we can't easily mock it unless we spy or it's in a
        // different package.
        // Actually, it's in the same package, so we can't really mock it here if we use
        // InjectMocks on the real service.
        // But we can mock the repository it uses!
        when(backupCodeRepository.findByUserIdAndCodeHashAndUsedFalse(eq(userId), anyString()))
                .thenReturn(Optional.of(new BackupCode()));

        assertTrue(twoFactorService.verifyTwoFactorCode(userId, "TESTCODE"));
    }

    private String generateCurrentCode(String secretBase32) throws NoSuchAlgorithmException, InvalidKeyException {
        Base32 base32 = new Base32();
        byte[] decoded = base32.decode(secretBase32);
        SecretKeySpec key = new SecretKeySpec(decoded, "HmacSHA1");
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30));
        return String.format("%06d", totp.generateOneTimePassword(key, Instant.now()));
    }
}
