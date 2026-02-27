package com.cryptodash.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour le Trading (Achat / Vente).
 * Vérifie que les soldes sont mis à jour après chaque opération.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradingE2ETest extends SeleniumBaseTest {

    @BeforeEach
    void loginForTrading() {
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);
    }

    @Test
    @Order(1)
    void testDepositAndBuyCrypto() {
        // 1. Dépôt de garantie pour s'assurer d'avoir des fonds
        navigateTo("/wallet");
        waitForClickable(By.xpath("//button[contains(., 'Déposer')]")).click();
        waitForClickable(By.xpath("//button[contains(., '1') and contains(., '000')]")).click();
        waitForElement(By.xpath("//button[contains(text(), 'Continuer vers le paiement')]")).click();

        waitForElement(By.cssSelector("input[name='cardName']")).sendKeys("Trader Test");
        driver.findElement(By.cssSelector("input[name='cardNumber']")).sendKeys("4242424242424242");
        driver.findElement(By.cssSelector("input[name='cardExpiry']")).sendKeys("12/28");
        driver.findElement(By.cssSelector("input[name='cardCvv']")).sendKeys("123");
        waitForClickable(By.xpath("//button[contains(text(), 'Payer')]")).click();
        waitForElement(By.xpath("//p[contains(text(), 'Dépôt confirmé')]"));

        // Récupérer le solde USDT initial
        navigateTo("/wallet");
        BigDecimal initialUsdt = getAssetBalance("USDT");

        // 2. Navigation vers la page de trading BTC
        navigateTo("/dashboard/BTC");
        waitForUrl("/dashboard/BTC");
        // Attendre que l'input soit présent (indique que tick est chargé)
        waitForElement(By.id("tradeAmount"));

        // 3. Achat de BTC
        driver.findElement(By.id("tradeAmount")).sendKeys("0.01");
        waitForClickable(By.xpath("//button[contains(text(), 'Acheter')]")).click();
        waitForElement(By.xpath("//p[contains(@class, 'text-emerald-400') and contains(text(), 'Achat effectué')]"));

        // 4. Vérification dans le portefeuille
        navigateTo("/wallet");
        BigDecimal afterBuyUsdt = getAssetBalance("USDT");

        assertThat(afterBuyUsdt).isLessThan(initialUsdt);

        BigDecimal btcBalance = getAssetBalance("BTC");
        assertThat(btcBalance).isPositive();
    }

    @Test
    @Order(2)
    void testSellCrypto() {
        // Pré-requis : on suppose avoir du BTC (le test précédent en a acheté)
        navigateTo("/wallet");
        BigDecimal initialBtc = getAssetBalance("BTC");
        BigDecimal initialUsdt = getAssetBalance("USDT");

        // 1. Navigation vers la page BTC
        navigateTo("/dashboard/BTC");
        waitForUrl("/dashboard/BTC");
        waitForElement(By.id("tradeAmount"));

        // 2. Vente de BTC
        driver.findElement(By.id("tradeAmount")).clear();
        driver.findElement(By.id("tradeAmount")).sendKeys("0.005");
        waitForClickable(By.xpath("//button[contains(text(), 'Vendre')]")).click();
        waitForElement(By.xpath("//p[contains(@class, 'text-emerald-400') and contains(text(), 'Vente effectuée')]"));

        // 3. Vérification du retour en USDT
        navigateTo("/wallet");
        BigDecimal afterSellUsdt = getAssetBalance("USDT");

        assertThat(afterSellUsdt).isGreaterThan(initialUsdt);

        BigDecimal afterSellBtc = getAssetBalance("BTC");
        assertThat(afterSellBtc).isLessThan(initialBtc);
    }

    private BigDecimal getAssetBalance(String symbol) {
        // Attendre que la section "Vos Actifs" soit chargée
        waitForElement(By.xpath("//h2[contains(text(), 'Vos Actifs')]"));

        // Cherche l'élément contenant le symbole exact
        WebElement symbolDiv = waitForElement(
                By.xpath("//div[contains(@class, 'font-semibold') and text()='" + symbol + "']"));

        // L'élément suivant (ou dans le même bloc parent) contient le montant sous la
        // forme "VALEUR · MONTANT SYMBOL"
        WebElement amountDiv = symbolDiv.findElement(By.xpath("../div[contains(@class, 'tabular-nums')]"));
        String text = amountDiv.getText();

        // Extraire la partie après le point médian "·"
        if (text.contains("·")) {
            text = text.split("·")[1].trim();
        }
        return new BigDecimal(cleanNumeric(text.split(" ")[0]));
    }
}
