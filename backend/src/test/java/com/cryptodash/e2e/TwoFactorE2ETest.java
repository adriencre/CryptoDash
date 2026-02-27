package com.cryptodash.e2e;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour la Double Authentification (2FA).
 * Vérifie l'activation, la déconnexion forcée, et le check 2FA au login.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TwoFactorE2ETest extends SeleniumBaseTest {

    private static String userEmail;
    private static final String PASSWORD = "SecurePassword123!";
    private static String totpSecret;
    private static List<String> backupCodes;

    @Test
    @Order(1)
    void testEnable2FA() throws Exception {
        // 1. Inscription d'un nouvel utilisateur
        userEmail = uniqueEmail();
        registerUser(userEmail, PASSWORD);

        // 2. Aller dans les Paramètres
        navigateTo("/settings");

        // 3. Cliquer sur Activer la 2FA
        waitForClickable(By.xpath("//button[contains(text(), 'Activer la 2FA')]")).click();

        // 4. Récupérer le secret affiché
        WebElement secretCode = waitForElement(By.cssSelector("code.break-all"));
        totpSecret = secretCode.getText().trim();
        assertThat(totpSecret).isNotEmpty();

        // 5. Générer un code TOTP valide
        String code = generateTotp(totpSecret);

        // 6. Saisir le code et confirmer
        WebElement input = driver.findElement(By.id("setup-code"));
        input.sendKeys(code);

        waitForClickable(By.xpath("//button[contains(text(), 'Confirmer')]")).click();

        // 7. Vérifier l'affichage des codes de secours
        waitForElement(By.xpath("//p[contains(text(), 'Enregistrez ces 12 codes de secours')]"));
        List<WebElement> codeElements = driver.findElements(By.xpath("//div[contains(@class, 'bg-slate-900/80')]"));
        assertThat(codeElements).hasSize(12);

        backupCodes = codeElements.stream().map(WebElement::getText).collect(Collectors.toList());

        // 8. Finaliser
        waitForClickable(By.xpath("//button[contains(text(), \"J'ai enregistré les codes\")]")).click();

        // Vérifier que c'est bien activé
        assertThat(waitForElement(By.xpath("//p[contains(text(), 'La double authentification est activée')]")))
                .isNotNull();

        logout();
    }

    @Test
    @Order(2)
    void testLoginWith2FA() throws Exception {
        // 1. Tentative de login normale (étape 1)
        navigateTo("/login");
        waitForElement(By.id("email")).sendKeys(userEmail);
        driver.findElement(By.id("password")).sendKeys(PASSWORD);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 2. Vérifier qu'on est à l'étape 2 (Code 2FA)
        waitForElement(By.xpath("//p[contains(text(), 'Entrez le code à 6 chiffres')]"));

        // 3. Saisir un code TOTP
        String code = generateTotp(totpSecret);
        WebElement codeInput = driver.findElement(By.id("code2fa"));
        codeInput.sendKeys(code);

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 4. Vérifier qu'on arrive sur le dashboard
        waitForUrl("/dashboard");
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    }

    @Test
    @Order(3)
    void testLoginWithBackupCode() {
        // 1. Étape 1 login
        navigateTo("/login");
        waitForElement(By.id("email")).sendKeys(userEmail);
        driver.findElement(By.id("password")).sendKeys(PASSWORD);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 2. Étape 2 : Saisir le PREMIER code de secours
        waitForElement(By.id("code2fa")).sendKeys(backupCodes.get(0));
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 3. Succès
        waitForUrl("/dashboard");
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    }

    private String generateTotp(String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secret);
        SecretKeySpec key = new SecretKeySpec(bytes, "HmacSHA1");
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30));
        int code = totp.generateOneTimePassword(key, Instant.now());
        return String.format("%06d", code);
    }
}
