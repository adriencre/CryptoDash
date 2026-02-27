package com.cryptodash.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour les transferts Peer-to-Peer (Envoi de crypto entre
 * utilisateurs).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class P2PTransferE2ETest extends SeleniumBaseTest {

    private static String recipientEmail;
    private static final String COMMON_PASSWORD = "TestPassword123!";

    @Test
    @Order(1)
    void testSendCryptoToAnotherUser() {
        // 1. Créer un compte destinataire (Utilisateur B)
        recipientEmail = uniqueEmail();
        registerUser(recipientEmail, COMMON_PASSWORD);
        logout();

        // 2. Se connecter avec l'expéditeur (Utilisateur A - compte test standard)
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);

        // S'assurer que l'expéditeur a assez d'USDT pour acheter du BTC
        navigateTo("/wallet");
        ensureUsdtBalance(new BigDecimal("1000"));

        // Acheter du BTC pour avoir un actif à envoyer (le modal P2P ne liste pas
        // l'USDT)
        navigateTo("/dashboard/BTC");
        waitForElement(By.id("tradeAmount"));
        driver.findElement(By.id("tradeAmount")).sendKeys("0.05");
        waitForClickable(By.xpath("//button[contains(text(), 'Acheter')]")).click();
        waitForElement(By.xpath("//p[contains(text(), 'Achat effectué')]"));

        navigateTo("/wallet");
        BigDecimal initialSenderBtc = getAssetBalance("BTC");

        // 3. Ouvrir le modal d'envoi et effectuer le transfert
        waitForClickable(By.xpath("//button[contains(text(), 'Envoyer')]")).click();

        WebElement recipientInput = waitForElement(By.id("send-recipient"));
        recipientInput.sendKeys(recipientEmail);

        // Sélectionner BTC
        WebElement symbolSelect = driver.findElement(By.id("send-symbol"));
        new Select(symbolSelect).selectByVisibleText("BTC");

        driver.findElement(By.id("send-amount")).sendKeys("0.02");

        // Cliquer sur envoyer (bouton spécifique dans le modal)
        waitForClickable(By.xpath("//button[@type='submit' and contains(., 'Envoyer')]")).click();

        // Vérifier le message de succès
        waitForElement(By.xpath("//p[contains(text(), 'Virement envoyé') or contains(text(), 'Transfert effectué')]"));

        // Attendre un peu que le solde se mette à jour ou rafraîchir
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
        } // Laisse le temps au modal de se fermer

        navigateTo("/wallet");
        BigDecimal afterSenderBtc = getAssetBalance("BTC");

        // Vérifier que le solde BTC a diminué
        assertThat(afterSenderBtc).isLessThan(initialSenderBtc.subtract(new BigDecimal("0.019")));

        logout();

        // 4. Se connecter avec le destinataire (Utilisateur B) et vérifier la réception
        login(recipientEmail, COMMON_PASSWORD);
        navigateTo("/wallet");

        BigDecimal recipientBtc = getAssetBalance("BTC");

        // Le destinataire doit avoir reçu exactement 0.02 BTC
        assertThat(recipientBtc.stripTrailingZeros()).isEqualTo(new BigDecimal("0.02").stripTrailingZeros());
    }

    /**
     * Helper pour extraire le montant d'un actif dans le tableau du wallet.
     */
    private BigDecimal getAssetBalance(String symbol) {
        waitForElement(By.xpath("//h2[contains(text(), 'Vos Actifs')]"));
        WebElement symbolDiv = waitForElement(
                By.xpath("//div[contains(@class, 'font-semibold') and text()='" + symbol + "']"));

        // Le montant est dans le div de classe tabular-nums juste à côté
        WebElement amountDiv = symbolDiv.findElement(By.xpath("../div[contains(@class, 'tabular-nums')]"));
        String text = amountDiv.getText();

        // Format attendu: "VALEUR_USDT · MONTANT SYMBOL"
        if (text.contains("·")) {
            text = text.split("·")[1].trim().split(" ")[0];
        }
        return new BigDecimal(cleanNumeric(text));
    }

    /**
     * S'assure que le compte a un solde minimum en déposant si nécessaire.
     */
    private void ensureUsdtBalance(BigDecimal minAmount) {
        BigDecimal current = getAssetBalance("USDT");
        if (current.compareTo(minAmount) < 0) {
            waitForClickable(By.xpath("//button[contains(text(), 'Déposer')]")).click();
            waitForElement(By.xpath("//button[contains(text(), '100')]")).click();
            waitForElement(By.xpath("//button[contains(text(), 'Continuer vers le paiement')]")).click();

            waitForElement(By.cssSelector("input[name='cardName']")).sendKeys("Expediteur Test");
            driver.findElement(By.cssSelector("input[name='cardNumber']")).sendKeys("4242424242424242");
            driver.findElement(By.cssSelector("input[name='cardExpiry']")).sendKeys("12/28");
            driver.findElement(By.cssSelector("input[name='cardCvv']")).sendKeys("123");

            waitForClickable(By.xpath("//button[contains(text(), 'Payer')]")).click();
            waitForElement(By.xpath("//p[contains(text(), 'Dépôt confirmé')]"));

            navigateTo("/wallet");
        }
    }
}
