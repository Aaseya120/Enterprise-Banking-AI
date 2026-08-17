package com.bank.ai.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bank")
public class AiOrchestratorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiOrchestratorServiceApplication.class, args);
    }
}
