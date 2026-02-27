package com.cryptodash.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour les Favoris (Watchlist).
 * Vérifie l'ajout, la suppression et la persistance des favoris.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FavoritesE2ETest extends SeleniumBaseTest {

    @BeforeEach
    void loginForFavorites() {
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        navigateTo("/dashboard");
    }

    @Test
    @Order(1)
    void testToggleFavoritePersistence() {
        // 1. Identifier le bouton favori pour le BTC (supposé non favori au départ)
        // Note: On utilise le symbole "BTC" qui est dans l'URL du lien de la carte
        By btcFavBtnLocator = By.xpath("//a[contains(@href, '/dashboard/BTC')]//button");

        WebElement favoriteBtn = waitForElement(btcFavBtnLocator);
        String initialLabel = favoriteBtn.getAttribute("aria-label");

        // S'il est déjà en favori (test relancé), on le retire d'abord pour avoir un
        // état propre
        if ("Retirer des favoris".equals(initialLabel)) {
            favoriteBtn.click();
            // Petite attente pour le traitement API
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            favoriteBtn = driver.findElement(btcFavBtnLocator);
        }

        assertThat(favoriteBtn.getAttribute("aria-label")).isEqualTo("Ajouter aux favoris");

        // 2. Ajouter aux favoris
        favoriteBtn.click();

        // Vérifier le changement immédiat du label (UI réactive)
        wait.until(d -> "Retirer des favoris".equals(d.findElement(btcFavBtnLocator).getAttribute("aria-label")));

        // 3. Rafraîchir la page pour vérifier la persistance (Backend DB)
        driver.navigate().refresh();

        favoriteBtn = waitForElement(btcFavBtnLocator);
        assertThat(favoriteBtn.getAttribute("aria-label"))
                .as("Le favori doit persister après rafraîchissement")
                .isEqualTo("Retirer des favoris");

        // 4. Retirer des favoris
        favoriteBtn.click();
        wait.until(d -> "Ajouter aux favoris".equals(d.findElement(btcFavBtnLocator).getAttribute("aria-label")));

        // 5. Rafraîchir de nouveau
        driver.navigate().refresh();
        favoriteBtn = waitForElement(btcFavBtnLocator);
        assertThat(favoriteBtn.getAttribute("aria-label"))
                .as("Le favori ne doit plus être présent après suppression et rafraîchissement")
                .isEqualTo("Ajouter aux favoris");
    }

    @Test
    @Order(2)
    void testFavoritesFilter() {
        // 1. S'assurer que seul le BTC est en favori
        // (On pourrait nettoyer tous les favoris via API, mais ici on va juste en
        // ajouter un)
        By btcFavBtnLocator = By.xpath("//a[contains(@href, '/dashboard/BTC')]//button");
        WebElement btcBtn = waitForElement(btcFavBtnLocator);
        if ("Ajouter aux favoris".equals(btcBtn.getAttribute("aria-label"))) {
            btcBtn.click();
        }

        // 2. Cliquer sur le filtre "Favoris seulement"
        WebElement filterBtn = waitForClickable(By.xpath("//button[contains(text(), 'Favoris seulement')]"));
        filterBtn.click();

        // 3. Vérifier que le BTC est présent et que d'autres (ex. ETH) ont disparu
        // On attend un peu que l'animation/filtrage se fasse
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        assertThat(driver.findElements(By.xpath("//a[contains(@href, '/dashboard/BTC')]"))).isNotEmpty();

        // On suppose que ETH existe dans la liste globale
        // Si ETH n'est pas en favori, il ne doit plus être visible
        By ethCardLocator = By.xpath("//a[contains(@href, '/dashboard/ETH')]");
        assertThat(driver.findElements(ethCardLocator)).isEmpty();

        // 4. Revenir sur "Tous"
        waitForClickable(By.xpath("//button[contains(text(), 'Tous')]")).click();
        assertThat(waitForElement(ethCardLocator).isDisplayed()).isTrue();
    }
}
