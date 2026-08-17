package com.bank.ai.mcp.controller;

import java.util.Map;
import java.util.Set;

public record ToolInvokeRequest(String toolName, Map<String, Object> arguments, String callerId, Set<String> callerRoles) {
}
