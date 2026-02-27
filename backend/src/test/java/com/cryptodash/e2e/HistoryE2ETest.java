package com.cryptodash.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour l'Historique des Transactions.
 * Vérifie que les actions effectuées créent bien des entrées détaillées dans
 * l'historique.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HistoryE2ETest extends SeleniumBaseTest {

    @BeforeEach
    void loginForHistory() {
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);
    }

    @Test
    @Order(1)
    void testTransactionAppearsInHistory() {
        // 1. Dépôt de garantie pour s'assurer d'avoir des fonds
        navigateTo("/wallet");
        waitForClickable(By.xpath("//button[contains(text(), 'Déposer')]")).click();
        waitForElement(By.xpath("//button[contains(text(), '500')]")).click();
        waitForElement(By.xpath("//button[contains(text(), 'Continuer vers le paiement')]")).click();

        waitForElement(By.cssSelector("input[name='cardName']")).sendKeys("History Tester");
        driver.findElement(By.cssSelector("input[name='cardNumber']")).sendKeys("4242424242424242");
        driver.findElement(By.cssSelector("input[name='cardExpiry']")).sendKeys("12/28");
        driver.findElement(By.cssSelector("input[name='cardCvv']")).sendKeys("123");
        waitForClickable(By.xpath("//button[contains(text(), 'Payer')]")).click();
        waitForElement(By.xpath("//p[contains(text(), 'Dépôt confirmé')]"));

        // 2. Navigation et achat d'une crypto (BTC)
        navigateTo("/dashboard/BTC");
        waitForUrl("/dashboard/BTC");
        // Attendre que l'input soit présent (indique que tick est chargé)
        waitForElement(By.id("tradeAmount"));

        waitForElement(By.id("tradeAmount")).sendKeys("0.002");
        waitForClickable(By.xpath("//button[contains(text(), 'Acheter')]")).click();
        waitForElement(By.xpath("//p[contains(text(), 'Achat effectué')]"));

        // 3. Aller sur la page Historique
        navigateTo("/history");
        // Petite pause pour laisser le temps à l'UI de charger la liste
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
        }
        waitForElement(By.xpath("//h1[contains(text(), 'Historique des transactions')]"));

        // Attendre que la liste soit peuplée
        wait.until(d -> !d.findElements(By.cssSelector("ul li")).isEmpty());

        // 4. Vérifier que la transaction d'achat est présente en haut de liste
        List<WebElement> transactions = driver.findElements(By.cssSelector("ul li"));
        WebElement latestTx = transactions.get(0);

        // Vérifier le libellé (Achat BTC)
        WebElement label = latestTx.findElement(By.cssSelector("p.font-medium.text-white"));
        assertThat(label.getText()).contains("Achat BTC");

        // Vérifier le montant (0,002 avec virgule en format fr-FR)
        WebElement amount = latestTx.findElement(By.cssSelector("p.tabular-nums"));
        assertThat(amount.getText()).contains("0,002");
        assertThat(amount.getText()).contains("BTC");
    }

    @Test
    @Order(2)
    void testDepositAppearsInHistory() {
        navigateTo("/history");
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
        }

        // Attendre que la liste soit peuplée
        wait.until(d -> !d.findElements(By.cssSelector("ul li")).isEmpty());

        // On cherche le libellé "Dépôt"
        boolean foundDeposit = false;
        List<WebElement> transactions = driver.findElements(By.cssSelector("ul li"));
        for (WebElement tx : transactions) {
            String txt = tx.getText();
            if (txt.contains("Dépôt") && txt.contains("USDT")) {
                foundDeposit = true;
                // Vérifier la couleur verte
                WebElement amount = tx.findElement(By.cssSelector("p.tabular-nums"));
                assertThat(amount.getAttribute("class")).contains("text-emerald-400");
                break;
            }
        }
        assertThat(foundDeposit).as("La transaction de dépôt doit être dans l'historique").isTrue();
    }
}
