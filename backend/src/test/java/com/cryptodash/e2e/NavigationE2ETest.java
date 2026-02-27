package com.cryptodash.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour la navigation dans la sidebar :
 * dashboard, wallet, historique.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NavigationE2ETest extends SeleniumBaseTest {

    private void loginFirst() {
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        waitForUrl("/dashboard");
    }

    @Test
    @Order(1)
    void testDashboardNavigation() {
        loginFirst();

        // Vérifier que le dashboard est affiché
        assertThat(driver.getCurrentUrl()).contains("/dashboard");

        // Vérifier la présence du header "Espace de trading"
        WebElement header = waitForElement(By.xpath("//span[contains(text(), 'Espace de trading')]"));
        assertThat(header.isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    void testNavigateToWallet() {
        loginFirst();

        // Cliquer sur le lien "Portefeuille" dans la sidebar
        WebElement walletLink = waitForClickable(
                By.xpath("//a[@href='/wallet']//span[contains(text(), 'Portefeuille')]/.."));
        walletLink.click();

        waitForUrl("/wallet");
        assertThat(driver.getCurrentUrl()).contains("/wallet");

        // Vérifier que le contenu wallet se charge
        WebElement walletTitle = waitForElement(By.xpath("//h1[contains(text(), 'Mon Portefeuille')]"));
        assertThat(walletTitle.isDisplayed()).isTrue();
    }

    @Test
    @Order(3)
    void testNavigateToHistory() {
        loginFirst();

        // Cliquer sur le lien "Historique" dans la sidebar
        WebElement historyLink = waitForClickable(
                By.xpath("//a[@href='/history']//span[contains(text(), 'Historique')]/.."));
        historyLink.click();

        waitForUrl("/history");
        assertThat(driver.getCurrentUrl()).contains("/history");
    }
}
