package com.cryptodash.service;

import com.cryptodash.entity.BackupCode;
import com.cryptodash.entity.User;
import com.cryptodash.repository.BackupCodeRepository;
import com.cryptodash.repository.UserRepository;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TwoFactorService {

    private static final int SECRET_BYTES = 20;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int BACKUP_CODE_COUNT = 12;
    private static final String BACKUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Duration TOTP_WINDOW = Duration.ofSeconds(30);

    private final UserRepository userRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final Base32 base32 = new Base32();

    public TwoFactorService(UserRepository userRepository, BackupCodeRepository backupCodeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.backupCodeRepository = backupCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Génère un secret TOTP et l'enregistre (sans activer la 2FA).
     * Retourne le secret Base32 et l'URL pour le QR code.
     */
    @Transactional
    public TwoFactorSetupResult setupTwoFactor(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("La double authentification est déjà activée.");
        }
        byte[] secretBytes = new byte[SECRET_BYTES];
        random.nextBytes(secretBytes);
        String secretBase32 = new String(base32.encode(secretBytes), StandardCharsets.UTF_8).replace("=", "");
        user.setTwoFactorSecret(secretBase32);
        userRepository.save(user);

        String issuer = "CryptoDash";
        String label = issuer + ":" + user.getEmail();
        String qrUrl = String.format("otpauth://totp/%s?secret=%s&issuer=%s", label.replace(":", "%3A"), secretBase32, issuer);
        return new TwoFactorSetupResult(secretBase32, qrUrl);
    }

    /**
     * Active la 2FA après vérification du code TOTP. Génère les 12 codes de secours.
     */
    @Transactional
    public List<String> enableTwoFactor(UUID userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("La double authentification est déjà activée.");
        }
        String secret = user.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Appelez d'abord /api/auth/2fa/setup.");
        }
        if (!verifyTotp(secret, code)) {
            throw new IllegalArgumentException("Code invalide. Vérifiez l'heure de votre appareil.");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        List<String> plainCodes = generateBackupCodes();
        Instant now = Instant.now();
        for (String plain : plainCodes) {
            BackupCode bc = new BackupCode();
            bc.setUser(user);
            bc.setCodeHash(hashBackupCode(plain));
            bc.setUsed(false);
            backupCodeRepository.save(bc);
        }
        return plainCodes;
    }

    /**
     * Désactive la 2FA après vérification du mot de passe et du code TOTP.
     */
    @Transactional
    public void disableTwoFactor(UUID userId, String password, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (!user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("La double authentification n'est pas activée.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe incorrect.");
        }
        if (!verifyTotp(user.getTwoFactorSecret(), code) && !consumeBackupCode(userId, code)) {
            throw new IllegalArgumentException("Code invalide.");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        backupCodeRepository.deleteByUserId(userId);
    }

    /**
     * Vérifie un code TOTP ou un code de secours lors du login. Si code de secours, il est consommé.
     */
    public boolean verifyTwoFactorCode(UUID userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (!user.isTwoFactorEnabled()) return true;
        if (verifyTotp(user.getTwoFactorSecret(), code)) return true;
        return consumeBackupCode(userId, code);
    }

    /**
     * Régénère les 12 codes de secours (après vérification mot de passe + TOTP).
     */
    @Transactional
    public List<String> regenerateBackupCodes(UUID userId, String password, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (!user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("Activez d'abord la double authentification.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe incorrect.");
        }
        if (!verifyTotp(user.getTwoFactorSecret(), code) && !consumeBackupCode(userId, code)) {
            throw new IllegalArgumentException("Code invalide.");
        }
        backupCodeRepository.deleteByUserId(userId);
        List<String> plainCodes = generateBackupCodes();
        for (String plain : plainCodes) {
            BackupCode bc = new BackupCode();
            bc.setUser(user);
            bc.setCodeHash(hashBackupCode(plain));
            bc.setUsed(false);
            backupCodeRepository.save(bc);
        }
        return plainCodes;
    }

    private boolean verifyTotp(String secretBase32, String code) {
        if (code == null || code.length() != 6) return false;
        try {
            byte[] decoded = base32.decode(secretBase32);
            SecretKeySpec key = new SecretKeySpec(decoded, "HmacSHA1");
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator(TOTP_WINDOW);
            Instant now = Instant.now();
            for (int i = -1; i <= 1; i++) {
                Instant t = now.plusSeconds(i * 30L);
                int expected = totp.generateOneTimePassword(key, t);
                if (String.format("%06d", expected).equals(code)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    protected boolean consumeBackupCode(UUID userId, String code) {
        if (code == null || code.length() != BACKUP_CODE_LENGTH) return false;
        String hash = hashBackupCode(code);
        var backup = backupCodeRepository.findByUserIdAndCodeHashAndUsedFalse(userId, hash);
        if (backup.isEmpty()) return false;
        BackupCode bc = backup.get();
        bc.setUsed(true);
        bc.setUsedAt(Instant.now());
        backupCodeRepository.save(bc);
        return true;
    }

    private List<String> generateBackupCodes() {
        List<String> list = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                sb.append(BACKUP_CODE_ALPHABET.charAt(random.nextInt(BACKUP_CODE_ALPHABET.length())));
            }
            list.add(sb.toString());
        }
        return list;
    }

    private String hashBackupCode(String plain) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            return new String(base32.encode(hash), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public record TwoFactorSetupResult(String secret, String qrCodeUrl) {}
}
