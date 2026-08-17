package com.bank.ai.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bank")
public class FraudAiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudAiServiceApplication.class, args);
    }
}
