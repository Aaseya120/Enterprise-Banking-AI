package com.bank.ai.orchestrator.service;

import com.bank.ai.gateway.client.AiModelClient;
import com.bank.ai.gateway.model.AiResponse;
import com.bank.ai.gateway.model.ModelProvider;
import com.bank.ai.gateway.router.ModelRouter;
import com.bank.ai.orchestrator.client.McpGatewayClient;
import com.bank.ai.orchestrator.client.RagServiceClient;
import com.bank.ai.orchestrator.model.ChatRequest;
import com.bank.ai.orchestrator.model.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Verifies the resilience behavior documented in AiOrchestratorService's
 * javadoc: when rag-service is down, the circuit breaker's fallback
 * (List.of()) kicks in and the chat request still succeeds with an
 * ungrounded answer, rather than the whole request failing.
 *
 * Uses a mock CircuitBreakerFactory rather than a real
 * Resilience4JCircuitBreakerFactory: the latter has no public constructor
 * outside of Spring Boot auto-configuration, and more importantly this test
 * is asserting AiOrchestratorService's fallback WIRING (the lambda passed
 * as the second argument to breaker.run()) not Resilience4j's breaker state
 * machine, which is Spring Cloud's and Resilience4j's own responsibility.
 */
@SuppressWarnings("unchecked")
class AiOrchestratorServiceCircuitBreakerTest {

    @Test
    void policyQuestionStillAnswersWhenRagServiceIsDown() {
        RagServiceClient ragServiceClient = mock(RagServiceClient.class);
        when(ragServiceClient.retrieve(any(), any(Integer.class), anySet()))
                .thenThrow(new RuntimeException("rag-service unreachable"));

        McpGatewayClient mcpGatewayClient = mock(McpGatewayClient.class);
        ModelRouter modelRouter = mock(ModelRouter.class);
        AiModelClient modelClient = mock(AiModelClient.class);
        when(modelRouter.selectModel(any())).thenReturn(modelClient);
        when(modelClient.generateWithContext(any(), any())).thenReturn(new AiResponse(
                "Personal loans generally require a good credit score.", ModelProvider.OPENAI,
                "gpt-4o (demo)", List.of(), 0, 0, 5L, false));

        ResponseHandlerService responseHandlerService = new ResponseHandlerService();
        IntentDetectionService intentDetectionService = new IntentDetectionService();

        // Stub breaker.run(callable, fallback): executes the callable; on any
        // exception it delegates to the fallback exactly as a real open/half-open
        // breaker would do. This is sufficient to verify our fallback lambda.
        CircuitBreaker mockBreaker = mock(CircuitBreaker.class);
        when(mockBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> callable = invocation.getArgument(0);
                    Function<Throwable, ?> fallback = invocation.getArgument(1);
                    try {
                        return callable.get();
                    } catch (Exception ex) {
                        return fallback.apply(ex);
                    }
                });

        CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
        when(factory.create(anyString())).thenReturn(mockBreaker);

        AiOrchestratorService service = new AiOrchestratorService(
                intentDetectionService, ragServiceClient, mcpGatewayClient, modelRouter,
                responseHandlerService, factory);

        ChatResponse response = service.chat(new ChatRequest(
                "C1", "CUST-1", Set.of("CUSTOMER"),
                "What are the eligibility rules for personal loans?", null));

        assertThat(response.answer()).contains("Personal loans generally require a good credit score.");
        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
    }
}
