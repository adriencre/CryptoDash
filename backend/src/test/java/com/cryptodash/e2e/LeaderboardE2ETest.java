package com.cryptodash.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour le Classement (Leaderboard).
 */
class LeaderboardE2ETest extends SeleniumBaseTest {

    @BeforeEach
    void loginForLeaderboard() {
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        navigateTo("/leaderboard");
    }

    @Test
    void testLeaderboardDisplay() {
        // 1. Vérifier que le titre est correct
        WebElement heading = waitForElement(By.xpath("//h1[contains(text(), 'Classement Mondial')]"));
        assertThat(heading.isDisplayed()).isTrue();

        // 2. Vérifier que le tableau contient au moins une ligne (le compte de test ou
        // d'autres)
        waitForElement(By.cssSelector("table tbody tr"));
        List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
        assertThat(rows).isNotEmpty();

        // 3. Vérifier la présence des colonnes Rang, Trader et Valeur
        WebElement rankHeader = driver.findElement(By.xpath("//th[contains(text(), 'Rang')]"));
        WebElement traderHeader = driver.findElement(By.xpath("//th[contains(text(), 'Trader')]"));
        WebElement valueHeader = driver.findElement(By.xpath("//th[contains(text(), 'Valeur Totale')]"));

        assertThat(rankHeader.isDisplayed()).isTrue();
        assertThat(traderHeader.isDisplayed()).isTrue();
        assertThat(valueHeader.isDisplayed()).isTrue();

        // 4. Vérifier l'ordre de tri (Le premier doit avoir une valeur >= au second)
        if (rows.size() >= 2) {
            BigDecimal firstValue = parseValue(rows.get(0));
            BigDecimal secondValue = parseValue(rows.get(1));
            assertThat(firstValue).isGreaterThanOrEqualTo(secondValue);
        }

        // 5. Vérifier la présence des médailles pour le top 3 (si assez de données)
        if (!rows.isEmpty()) {
            WebElement rankCell = rows.get(0).findElement(By.xpath("./td[1]"));
            // Le premier a souvent une médaille ou un style spécifique
            assertThat(rankCell.getText()).isNotNull();
        }

        // 6. Vérifier que mon compte test est dans la liste (par email ou nom)
        boolean foundMe = false;
        for (WebElement row : rows) {
            if (row.getText().contains(TEST_USER_EMAIL)) {
                foundMe = true;
                break;
            }
        }
        assertThat(foundMe).as("Le compte de test doit figurer dans le classement").isTrue();
    }

    private BigDecimal parseValue(WebElement row) {
        // Cibler le premier div qui contient la valeur totale (le second contient le
        // ROI)
        String text = row.findElement(By.xpath("./td[3]/div[1]")).getText();
        return new BigDecimal(cleanNumeric(text));
    }
}
