package com.cryptodash.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour la page Wallet :
 * chargement, solde initial USDT, flux de dépôt.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WalletE2ETest extends SeleniumBaseTest {

    private void loginAndGoToWallet() {
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        navigateTo("/wallet");
        // Attendre que la page wallet se charge (section "Vos Actifs")
        waitForElement(By.xpath("//h2[contains(text(), 'Vos Actifs')]"));
    }

    @Test
    @Order(1)
    void testWalletPageLoads() {
        loginAndGoToWallet();

        WebElement heading = waitForElement(By.xpath("//h1[contains(text(), 'Mon Portefeuille')]"));
        assertThat(heading.isDisplayed()).isTrue();

        WebElement assetsSection = waitForElement(By.xpath("//h2[contains(text(), 'Vos Actifs')]"));
        assertThat(assetsSection.isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    void testInitialUsdtBalance() {
        loginAndGoToWallet();

        // Chercher la ligne USDT dans les positions
        WebElement usdtRow = waitForElement(
                By.xpath("//div[contains(@class, 'font-semibold') and contains(text(), 'USDT')]"));
        assertThat(usdtRow.isDisplayed()).isTrue();
    }

    @Test
    @Order(3)
    void testDepositFlow() {
        loginAndGoToWallet();

        // Cliquer sur le bouton "Déposer"
        WebElement depositBtn = waitForClickable(By.xpath("//button[contains(text(), 'Déposer')]"));
        depositBtn.click();

        // Vérifier que le modal de dépôt est ouvert
        WebElement modalTitle = waitForElement(By.xpath("//h2[contains(text(), 'Ajouter des USDT')]"));
        assertThat(modalTitle.isDisplayed()).isTrue();

        // Sélectionner un preset de 500
        WebElement preset500 = waitForClickable(By.xpath("//button[contains(text(), '500')]"));
        preset500.click();

        // Continuer vers le paiement
        WebElement continueBtn = waitForClickable(By.xpath("//button[contains(text(), 'Continuer vers le paiement')]"));
        continueBtn.click();

        // Remplir les informations de carte (simulé)
        waitForElement(By.cssSelector("input[name='cardName']")).sendKeys("Test User");
        driver.findElement(By.cssSelector("input[name='cardNumber']")).sendKeys("4242424242424242");
        driver.findElement(By.cssSelector("input[name='cardExpiry']")).sendKeys("12/28");
        driver.findElement(By.cssSelector("input[name='cardCvv']")).sendKeys("123");

        // Soumettre le paiement
        WebElement payBtn = waitForClickable(By.xpath("//button[contains(text(), 'Payer')]"));
        payBtn.click();

        // Vérifier le message de succès
        WebElement successMsg = waitForElement(By.xpath("//p[contains(text(), 'Dépôt confirmé')]"));
        assertThat(successMsg.isDisplayed()).isTrue();
    }
}
