package com.bank.ai.mcp.tool;

import java.util.Map;

public record ToolInvocationResult(String toolName, boolean success, Map<String, Object> data, String error) {
    public static ToolInvocationResult ok(String toolName, Map<String, Object> data) {
        return new ToolInvocationResult(toolName, true, data, null);
    }

    public static ToolInvocationResult failed(String toolName, String error) {
        return new ToolInvocationResult(toolName, false, Map.of(), error);
    }
}
