package com.bank.ai.mcp.tool.impl;

import com.bank.ai.mcp.client.AccountServiceClient;
import com.bank.ai.mcp.tool.McpTool;
import com.bank.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GetCustomerAccountsTool implements McpTool {

    private final AccountServiceClient accountServiceClient;

    public GetCustomerAccountsTool(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    @Override
    public String name() {
        return "getCustomerAccounts";
    }

    @Override
    public String description() {
        return "Lists all accounts belonging to a customerId (account type, status, balance).";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of("CUSTOMER", "BANK_STAFF", "ANALYST");
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> arguments, String callerId, Set<String> callerRoles) {
        String customerId = (String) arguments.get("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw BusinessException.validation("customerId is required");
        }
        List<Map<String, Object>> accounts = accountServiceClient.getAccountsForCustomer(customerId);
        return Map.of("accounts", accounts);
    }
}
