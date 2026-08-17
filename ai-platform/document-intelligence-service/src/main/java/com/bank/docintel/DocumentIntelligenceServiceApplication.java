package com.bank.docintel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bank")
public class DocumentIntelligenceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentIntelligenceServiceApplication.class, args);
    }
}
