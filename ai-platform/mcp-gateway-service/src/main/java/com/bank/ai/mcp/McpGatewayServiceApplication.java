package com.bank.ai.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bank")
public class McpGatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpGatewayServiceApplication.class, args);
    }
}
