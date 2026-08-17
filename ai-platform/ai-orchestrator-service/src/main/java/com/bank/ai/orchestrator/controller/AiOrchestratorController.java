package com.bank.ai.orchestrator.controller;

import com.bank.ai.orchestrator.model.ChatRequest;
import com.bank.ai.orchestrator.model.ChatResponse;
import com.bank.ai.orchestrator.service.AiOrchestratorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiOrchestratorController {

    private final AiOrchestratorService orchestratorService;

    public AiOrchestratorController(AiOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return orchestratorService.chat(request);
    }

    // /assist, /analyze, /summarize (section 5) would follow the same pattern,
    // typically routing to different system prompts / taskType metadata and,
    // for /analyze and /summarize, to the asynchronous job flow in section 47
    // rather than a synchronous response.
}
