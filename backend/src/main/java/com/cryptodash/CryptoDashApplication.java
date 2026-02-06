package com.cryptodash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoDashApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoDashApplication.class, args);
    }
}
