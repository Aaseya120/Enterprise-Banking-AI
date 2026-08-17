package com.bank.ai.orchestrator.service;

import com.bank.ai.gateway.model.AiRequest;
import com.bank.ai.gateway.model.AiResponse;
import com.bank.ai.gateway.model.DocumentChunk;
import com.bank.ai.gateway.router.ModelRouter;
import com.bank.ai.orchestrator.client.McpGatewayClient;
import com.bank.ai.orchestrator.client.RagServiceClient;
import com.bank.ai.orchestrator.model.ChatRequest;
import com.bank.ai.orchestrator.model.ChatResponse;
import com.bank.ai.orchestrator.model.Intent;
import com.bank.common.exception.AiPlatformException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The AI Orchestrator (section 5): the single entry point for all AI
 * requests. Implements, in order: intent detection -> RAG/tool selection ->
 * context retrieval -> prompt construction (delegated to ai-model-gateway)
 * -> LLM call (with a fallback provider on failure) -> response handling.
 *
 * Never talks to any banking database directly -- account data comes back
 * only through mcp-gateway-service's authorized tool calls, and policy/FAQ
 * context only through rag-service's retrieval endpoint.
 *
 * Both of those downstream calls go through a circuit breaker (see
 * CircuitBreakerConfiguration, plan section 29): if rag-service or
 * mcp-gateway-service is failing or slow, the breaker opens after enough
 * recent failures and this service stops hammering it, failing fast
 * instead. What happens on open/timeout differs deliberately by call --
 * see the fallback methods below.
 */
@Service
public class AiOrchestratorService {

    private static final String SYSTEM_PROMPT =
            "You are a helpful, careful banking assistant. Only state facts that are supported by the "
                    + "provided context or tool results. If you are not certain, say so and suggest the "
                    + "customer contact a bank representative. Never invent account numbers, balances, or policy terms.";

    private final IntentDetectionService intentDetectionService;
    private final RagServiceClient ragServiceClient;
    private final McpGatewayClient mcpGatewayClient;
    private final ModelRouter modelRouter;
    private final ResponseHandlerService responseHandlerService;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public AiOrchestratorService(IntentDetectionService intentDetectionService,
                                  RagServiceClient ragServiceClient,
                                  McpGatewayClient mcpGatewayClient,
                                  ModelRouter modelRouter,
                                  ResponseHandlerService responseHandlerService,
                                  CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.intentDetectionService = intentDetectionService;
        this.ragServiceClient = ragServiceClient;
        this.mcpGatewayClient = mcpGatewayClient;
        this.modelRouter = modelRouter;
        this.responseHandlerService = responseHandlerService;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public ChatResponse chat(ChatRequest request) {
        Intent intent = intentDetectionService.classify(request.query());

        return switch (intent) {
            case ACCOUNT_LOOKUP -> handleAccountLookup(request, intent);
            case POLICY_OR_FAQ -> handlePolicyOrFaq(request, intent);
            case GENERAL -> handleGeneral(request, intent);
        };
    }

    /**
     * Intent -> MCP tool call. AI never queries the account database itself
     * (section 16/31). Deliberately NO silent fallback here: a balance
     * either comes back correctly or the customer is told the service is
     * unavailable -- fabricating/guessing a balance when mcp-gateway-service
     * is down would be a financial-correctness violation (section 44), not
     * a graceful degradation.
     */
    private ChatResponse handleAccountLookup(ChatRequest request, Intent intent) {
        String accountId = request.context() != null ? (String) request.context().get("accountId") : null;
        if (accountId == null) {
            return new ChatResponse(request.conversationId(),
                    "I can look up your balance -- could you confirm which account (or the account ID)?",
                    intent, List.of(), true, "none");
        }

        var breaker = circuitBreakerFactory.create("mcp-gateway-service");
        Map<String, Object> toolResult = breaker.run(
                () -> mcpGatewayClient.invokeTool("getAccountBalance", Map.of("accountId", accountId),
                        request.userId(), request.userRoles()),
                throwable -> {
                    throw AiPlatformException.toolInvocationError(
                            "Account lookup is temporarily unavailable -- please try again shortly "
                                    + "or contact a bank representative.", throwable);
                });

        boolean success = Boolean.TRUE.equals(toolResult.get("success"));
        if (!success) {
            throw AiPlatformException.toolInvocationError(
                    "Unable to retrieve account balance: " + toolResult.get("error"), null);
        }

        Map<?, ?> data = (Map<?, ?>) toolResult.get("data");
        String answer = "Your current balance is " + data.get("balance") + " " + data.get("currency") + ".";
        return new ChatResponse(request.conversationId(), answer, intent, List.of("account-service"), true, "tool-call");
    }

    /**
     * Intent -> RAG retrieval -> grounded generation (sections 10, 53).
     * Unlike account lookup, a RAG failure degrades gracefully: an empty
     * context list is a valid input to generateWithFallback (the model
     * just answers ungrounded), and ResponseHandlerService already adds a
     * "not verified" disclaimer whenever grounded==false. Losing
     * rag-service temporarily means slightly worse answers, not a broken
     * feature -- so the circuit breaker's fallback returns List.of()
     * instead of propagating the failure.
     */
    private ChatResponse handlePolicyOrFaq(ChatRequest request, Intent intent) {
        var breaker = circuitBreakerFactory.create("rag-service");
        List<DocumentChunk> context = breaker.run(
                () -> ragServiceClient.retrieve(request.query(), 5, request.userRoles()),
                throwable -> List.of());

        AiRequest aiRequest = new AiRequest(
                request.conversationId(), request.userId(), "BANKING_ASSISTANT_V1", "1",
                SYSTEM_PROMPT, request.query(), List.of(), 0.2, 1024,
                Map.of("taskType", "SIMPLE_FAQ"));

        AiResponse aiResponse = generateWithFallback(aiRequest, context);
        String finalAnswer = responseHandlerService.finalize(aiResponse, !context.isEmpty());
        List<String> sources = context.stream().map(DocumentChunk::documentId).distinct().collect(Collectors.toList());

        return new ChatResponse(request.conversationId(), finalAnswer, intent, sources,
                aiResponse.grounded(), aiResponse.provider().name());
    }

    private ChatResponse handleGeneral(ChatRequest request, Intent intent) {
        AiRequest aiRequest = new AiRequest(
                request.conversationId(), request.userId(), "BANKING_ASSISTANT_V1", "1",
                SYSTEM_PROMPT, request.query(), List.of(), 0.3, 512, Map.of("taskType", "DEFAULT"));

        AiResponse aiResponse = generateWithFallback(aiRequest, List.of());
        String finalAnswer = responseHandlerService.finalize(aiResponse, false);

        return new ChatResponse(request.conversationId(), finalAnswer, intent, List.of(),
                aiResponse.grounded(), aiResponse.provider().name());
    }

    /** Model Gateway call with fallback-provider retry (section 7/37). */
    private AiResponse generateWithFallback(AiRequest aiRequest, List<DocumentChunk> context) {
        try {
            var client = modelRouter.selectModel(aiRequest);
            return client.generateWithContext(aiRequest, context);
        } catch (AiPlatformException primaryFailure) {
            try {
                var fallback = modelRouter.fallbackModel(aiRequest);
                return fallback.generateWithContext(aiRequest, context);
            } catch (Exception fallbackFailure) {
                throw AiPlatformException.providerError(
                        "Both primary and fallback model providers failed", fallbackFailure);
            }
        }
    }
}

