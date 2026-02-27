package com.cryptodash.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests E2E pour les flux d'authentification :
 * inscription, connexion, échec de connexion, déconnexion.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthE2ETest extends SeleniumBaseTest {

    private static String testEmail;
    private static final String TEST_PASSWORD = "TestPassword123!";

    @Test
    @Order(1)
    void testRegisterNewUser() {
        testEmail = uniqueEmail();
        navigateTo("/register");

        // Vérifier que la page d'inscription est chargée
        WebElement heading = waitForElement(By.cssSelector("h1"));
        assertThat(heading.getText()).contains("Créer un compte");

        // Remplir le formulaire
        driver.findElement(By.id("email")).sendKeys(testEmail);
        driver.findElement(By.id("password")).sendKeys(TEST_PASSWORD);

        // Soumettre
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Vérifier la redirection vers le dashboard
        waitForUrl("/dashboard");
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    }

    @Test
    @Order(2)
    void testLoginSuccess() {
        // Se connecter avec le compte de test standard
        navigateTo("/login");
        waitForElement(By.id("email")).sendKeys(TEST_USER_EMAIL);
        driver.findElement(By.id("password")).sendKeys(TEST_USER_PASSWORD);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        waitForUrl("/dashboard");
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    }

    @Test
    @Order(3)
    void testLoginFailure() {
        navigateTo("/login");

        waitForElement(By.id("email")).sendKeys("nonexistent@test.com");
        driver.findElement(By.id("password")).sendKeys("wrongPassword");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Vérifier l'affichage du message d'erreur
        WebElement errorMsg = waitForElement(By.cssSelector(".text-rose-400"));
        assertThat(errorMsg.getText()).isNotEmpty();

        // On reste sur la page login
        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    @Test
    @Order(4)
    void testLogoutRedirect() {
        // Se connecter d'abord
        login(TEST_USER_EMAIL, TEST_USER_PASSWORD);

        // Vérifier qu'on est sur le dashboard
        waitForUrl("/dashboard");

        // Cliquer sur le bouton Déconnexion dans la sidebar
        WebElement logoutBtn = waitForClickable(By.xpath("//button[contains(text(), 'Déconnexion')]"));
        logoutBtn.click();

        // Vérifier la redirection vers /login
        waitForUrl("/login");
        assertThat(driver.getCurrentUrl()).contains("/login");
    }
}
