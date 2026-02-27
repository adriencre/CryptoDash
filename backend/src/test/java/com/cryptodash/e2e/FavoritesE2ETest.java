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
        // Nettoyer les favoris existants pour avoir un état propre
        cleanupFavorites();
    }

    private void cleanupFavorites() {
        try {
            // Attendre que la page charge
            Thread.sleep(1000);
            
            // Chercher tous les boutons "Retirer des favoris" et cliquer dessus
            var removeButtons = driver.findElements(By.xpath("//button[@aria-label='Retirer des favoris']"));
            for (WebElement btn : removeButtons) {
                try {
                    btn.click();
                    Thread.sleep(500); // Petite attente entre chaque clic
                } catch (Exception e) {
                    // Ignorer les erreurs
                }
            }
            
            // Rafraîchir pour s'assurer que tout est propre
            if (!removeButtons.isEmpty()) {
                driver.navigate().refresh();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            // Ignorer les erreurs de cleanup
        }
    }

    @Test
    @Order(1)
    //@Disabled("Test désactivé temporairement - problème de persistance des favoris entre tests")
    @SuppressWarnings("deprecation")
    void testToggleFavoritePersistence() {
        // 1. Identifier le bouton favori pour le BTC
        By btcFavBtnLocator = By.xpath("//a[contains(@href, '/dashboard/BTC')]//button");

        WebElement favoriteBtn = waitForElement(btcFavBtnLocator);
        String initialLabel = favoriteBtn.getAttribute("aria-label");
        
        // 2. Ajouter aux favoris (si pas déjà fait)
        if ("Ajouter aux favoris".equals(initialLabel)) {
            favoriteBtn.click();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        // 3. Rafraîchir pour vérifier que le favori est bien là
        driver.navigate().refresh();
        favoriteBtn = waitForElement(btcFavBtnLocator);
        assertThat(favoriteBtn.getAttribute("aria-label"))
                .as("Le BTC doit être en favori")
                .isEqualTo("Retirer des favoris");

        // 4. Retirer des favoris
        favoriteBtn.click();
        
        // Attendre que l'UI se mette à jour
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        // 5. Rafraîchir pour vérifier la suppression
        driver.navigate().refresh();
        favoriteBtn = waitForElement(btcFavBtnLocator);
        assertThat(favoriteBtn.getAttribute("aria-label"))
                .as("Le favori ne doit plus être présent après suppression")
                .isEqualTo("Ajouter aux favoris");
    }

    @Test
    @Order(2)
    @SuppressWarnings("deprecation")
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
