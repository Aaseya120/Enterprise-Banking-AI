package com.bank.ai.orchestrator.model;

/**
 * Coarse intent categories driving the "RAG / Tool Selection" branch in the
 * orchestrator flow (section 5). ACCOUNT_LOOKUP routes to an MCP tool call;
 * everything else routes to RAG-grounded generation.
 */
public enum Intent {
    ACCOUNT_LOOKUP,
    POLICY_OR_FAQ,
    GENERAL
}
