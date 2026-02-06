package com.cryptodash.service;

import com.cryptodash.dto.AuthResponse;
import com.cryptodash.dto.LoginRequest;
import com.cryptodash.dto.LoginResponse;
import com.cryptodash.dto.ProfileResponse;
import com.cryptodash.dto.RegisterRequest;
import com.cryptodash.dto.TwoFactorStatusResponse;
import com.cryptodash.dto.UpdateProfileRequest;
import com.cryptodash.entity.User;
import com.cryptodash.repository.UserRepository;
import com.cryptodash.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TwoFactorService twoFactorService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.twoFactorService = twoFactorService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect.");
        }
        if (user.isTwoFactorEnabled()) {
            String tempToken = jwtService.generateTempToken(user.getId());
            return LoginResponse.with2FARequired(tempToken, user.getId(), user.getEmail());
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return LoginResponse.withToken(token, user.getId(), user.getEmail());
    }

    public AuthResponse verify2FA(String tempToken, String code) {
        if (jwtService.isTempToken(tempToken)) {
            UUID userId = jwtService.getUserIdFromToken(tempToken);
            if (twoFactorService.verifyTwoFactorCode(userId, code)) {
                User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
                String token = jwtService.generateToken(user.getId(), user.getEmail());
                return new AuthResponse(token, user.getId(), user.getEmail());
            }
        }
        throw new IllegalArgumentException("Code invalide ou expiré.");
    }

    public TwoFactorStatusResponse get2FAStatus(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return new TwoFactorStatusResponse(user.isTwoFactorEnabled());
    }

    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return new ProfileResponse(
                user.getEmail(),
                user.getAccountName(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAddressLine1(),
                user.getAddressLine2(),
                user.getPostalCode(),
                user.getCity(),
                user.getCountry()
        );
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Un autre compte utilise déjà cet email.");
            }
            user.setEmail(email);
        }
        if (request.accountName() != null) {
            String an = request.accountName().trim();
            if (an.isEmpty()) {
                user.setAccountName(null);
            } else {
                if (!an.matches("^[a-zA-Z0-9_]{2,50}$")) {
                    throw new IllegalArgumentException("Le nom de compte doit contenir entre 2 et 50 caractères (lettres, chiffres, underscore).");
                }
                String anLower = an.toLowerCase();
                if (!anLower.equals(user.getAccountName() == null ? null : user.getAccountName().toLowerCase()) && userRepository.existsByAccountName(anLower)) {
                    throw new IllegalArgumentException("Ce nom de compte est déjà utilisé.");
                }
                user.setAccountName(anLower);
            }
        }
        user.setFirstName(trimOrNull(request.firstName()));
        user.setLastName(trimOrNull(request.lastName()));
        user.setPhone(trimOrNull(request.phone()));
        user.setAddressLine1(trimOrNull(request.addressLine1()));
        user.setAddressLine2(trimOrNull(request.addressLine2()));
        user.setPostalCode(trimOrNull(request.postalCode()));
        user.setCity(trimOrNull(request.city()));
        user.setCountry(trimOrNull(request.country()));
        user = userRepository.save(user);
        return new ProfileResponse(
                user.getEmail(),
                user.getAccountName(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAddressLine1(),
                user.getAddressLine2(),
                user.getPostalCode(),
                user.getCity(),
                user.getCountry()
        );
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
