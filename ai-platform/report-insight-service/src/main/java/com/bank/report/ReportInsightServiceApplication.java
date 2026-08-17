package com.bank.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bank")
public class ReportInsightServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportInsightServiceApplication.class, args);
    }
}
