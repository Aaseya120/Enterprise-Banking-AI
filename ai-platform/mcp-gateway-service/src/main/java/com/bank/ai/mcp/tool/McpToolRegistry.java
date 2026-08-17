package com.bank.ai.mcp.tool;

import com.bank.common.exception.AiPlatformException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers all McpTool beans and is the single choke point through which
 * the AI orchestrator invokes any tool. Enforces the allowedRoles() check
 * before delegating -- this is the "Tool allowlists" guardrail from
 * section 27.
 */
@Component
public class McpToolRegistry {

    private final Map<String, McpTool> toolsByName;

    public McpToolRegistry(List<McpTool> tools) {
        this.toolsByName = tools.stream().collect(Collectors.toMap(McpTool::name, t -> t));
    }

    public List<Map<String, Object>> listTools() {
        return toolsByName.values().stream()
                .map(t -> Map.<String, Object>of(
                        "name", t.name(),
                        "description", t.description(),
                        "allowedRoles", t.allowedRoles()))
                .toList();
    }

    public ToolInvocationResult invoke(String toolName, Map<String, Object> arguments,
                                        String callerId, Set<String> callerRoles) {
        McpTool tool = toolsByName.get(toolName);
        if (tool == null) {
            return ToolInvocationResult.failed(toolName, "Unknown tool: " + toolName);
        }
        boolean authorized = callerRoles.stream().anyMatch(tool.allowedRoles()::contains);
        if (!authorized) {
            return ToolInvocationResult.failed(toolName,
                    "Caller roles " + callerRoles + " not authorized for tool " + toolName);
        }
        try {
            Map<String, Object> data = tool.invoke(arguments, callerId, callerRoles);
            return ToolInvocationResult.ok(toolName, data);
        } catch (Exception e) {
            throw AiPlatformException.toolInvocationError(
                    "Tool " + toolName + " invocation failed: " + e.getMessage(), e);
        }
    }
}
