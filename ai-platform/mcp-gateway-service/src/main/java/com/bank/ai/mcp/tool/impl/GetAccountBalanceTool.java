package com.bank.ai.mcp.tool.impl;

import com.bank.ai.mcp.client.AccountServiceClient;
import com.bank.ai.mcp.tool.McpTool;
import com.bank.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class GetAccountBalanceTool implements McpTool {

    private final AccountServiceClient accountServiceClient;

    public GetAccountBalanceTool(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    @Override
    public String name() {
        return "getAccountBalance";
    }

    @Override
    public String description() {
        return "Returns the current balance and currency for a given accountId owned by the caller.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of("CUSTOMER", "BANK_STAFF", "ANALYST");
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> arguments, String callerId, Set<String> callerRoles) {
        String accountId = (String) arguments.get("accountId");
        if (accountId == null || accountId.isBlank()) {
            throw BusinessException.validation("accountId is required");
        }
        // NOTE: a real implementation checks account.customerId == callerId (or staff/analyst
        // role) before returning data -- this is the "authorization" step referenced in
        // section 16's tool flow diagram.
        return accountServiceClient.getBalance(accountId);
    }
}
