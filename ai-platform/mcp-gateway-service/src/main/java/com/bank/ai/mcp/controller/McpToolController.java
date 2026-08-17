package com.bank.ai.mcp.controller;

import com.bank.ai.mcp.tool.McpToolRegistry;
import com.bank.ai.mcp.tool.ToolInvocationResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HTTP surface for the MCP gateway. The ai-orchestrator-service is the only
 * intended caller of /invoke -- it is what stands between the LLM's tool-call
 * decision and this registry (see ai-orchestrator's ToolInvokingChatService).
 */
@RestController
@RequestMapping("/api/v1/mcp")
public class McpToolController {

    private final McpToolRegistry registry;

    public McpToolController(McpToolRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/tools")
    public List<Map<String, Object>> listTools() {
        return registry.listTools();
    }

    @PostMapping("/invoke")
    public ToolInvocationResult invoke(@RequestBody ToolInvokeRequest request) {
        return registry.invoke(request.toolName(), request.arguments(), request.callerId(), request.callerRoles());
    }
}
