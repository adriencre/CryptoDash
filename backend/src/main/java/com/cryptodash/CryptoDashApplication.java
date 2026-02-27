package com.cryptodash;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoDashApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        String coingeckoApiKey = dotenv.get("COINGECKO_API_KEY");
        if (coingeckoApiKey != null && !coingeckoApiKey.trim().isEmpty()
                && System.getProperty("COINGECKO_API_KEY") == null
                && System.getenv("COINGECKO_API_KEY") == null) {
            System.setProperty("COINGECKO_API_KEY", coingeckoApiKey);
            System.out.println("COINGECKO_API_KEY loaded from .env: " + coingeckoApiKey.substring(0, Math.min(10, coingeckoApiKey.length())) + "...");
        } else {
            System.out.println("COINGECKO_API_KEY not loaded from .env (null/empty or already set)");
        }

        SpringApplication.run(CryptoDashApplication.class, args);
    }
}
