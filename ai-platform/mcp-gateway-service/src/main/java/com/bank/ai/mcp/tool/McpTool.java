package com.bank.ai.mcp.tool;

import java.util.Map;

/**
 * A single named, invokable capability exposed to the AI layer. Tools are
 * READ-oriented by design here (getAccountBalance, getTransactionHistory,
 * ...); a financial write (transfer, payment) is deliberately NOT modeled
 * as an McpTool -- those go through TransferService's own
 * authorize -> validate -> confirm -> OTP flow, never through the LLM
 * (architecture plan section 17).
 */
public interface McpTool {

    /** Unique tool name the AI orchestrator references, e.g. "getAccountBalance". */
    String name();

    String description();

    /** Roles allowed to invoke this tool (simple allowlist demo of section 16's security rule). */
    java.util.Set<String> allowedRoles();

    Map<String, Object> invoke(Map<String, Object> arguments, String callerId, java.util.Set<String> callerRoles);
}
