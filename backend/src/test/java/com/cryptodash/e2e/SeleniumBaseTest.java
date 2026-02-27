package com.cryptodash.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Classe de base pour tous les tests E2E Selenium.
 * Configure Chrome en mode headless et fournit des méthodes utilitaires.
 */
public abstract class SeleniumBaseTest {

    protected static final String BASE_URL = "http://localhost:4200";
    protected static final String TEST_USER_EMAIL = "test@example.com";
    protected static final String TEST_USER_PASSWORD = "TestPassword123!";
    protected static final Duration TIMEOUT = Duration.ofSeconds(20);

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeAll
    static void setupDriver() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=en-US");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, TIMEOUT);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void navigateTo(String path) {
        driver.get(BASE_URL + path);
    }

    protected WebElement waitForElement(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void waitForUrl(String urlPart) {
        wait.until(ExpectedConditions.urlContains(urlPart));
    }

    /**
     * Inscrit un nouvel utilisateur et retourne l'email utilisé.
     */
    protected String registerUser(String email, String password) {
        navigateTo("/register");
        waitForElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        waitForClickable(By.cssSelector("button[type='submit']")).click();
        waitForUrl("/dashboard");
        return email;
    }

    /**
     * Connecte un utilisateur existant.
     */
    protected void login(String email, String password) {
        navigateTo("/login");
        waitForElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        waitForClickable(By.cssSelector("button[type='submit']")).click();
        waitForUrl("/dashboard");
    }

    protected void logout() {
        navigateTo("/dashboard");
        // Si on est déjà redirigé vers /login, on ne fait rien
        if (driver.getCurrentUrl().contains("/login")) {
            return;
        }
        try {
            waitForClickable(By.xpath("//button[contains(text(), 'Déconnexion')]")).click();
            waitForUrl("/login");
        } catch (Exception e) {
            // Déjà déconnecté ou bouton absent
        }
    }

    /**
     * Génère un email unique pour éviter les collisions entre tests.
     */
    protected String uniqueEmail() {
        return "e2e-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000) + "@test.com";
    }

    /**
     * Nettoie une chaîne numérique contenant des séparateurs de milliers (espaces,
     * virgules, etc.)
     * et normalise le point décimal pour BigDecimal.
     */
    protected String cleanNumeric(String text) {
        if (text == null || text.trim().isEmpty())
            return "0";
        // Supprime tout ce qui n'est pas chiffre, virgule, point ou signe moins
        // Nettoie aussi les espaces insécables (u00A0, u202F) rencontrés dans les logs
        String cleaned = text.replaceAll("[^0-9,.-]", "").replace(",", ".");

        // S'il y a plusieurs points, on ne garde que le DERNIER comme séparateur
        // décimal
        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot != -1) {
            String integerPart = cleaned.substring(0, lastDot).replace(".", "");
            String decimalPart = cleaned.substring(lastDot + 1).replace(".", "");
            cleaned = integerPart + "." + decimalPart;
        }
        return cleaned.isEmpty() ? "0" : cleaned;
    }
}
