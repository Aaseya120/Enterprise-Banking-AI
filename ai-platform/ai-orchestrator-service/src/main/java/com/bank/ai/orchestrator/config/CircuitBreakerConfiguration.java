package com.bank.ai.orchestrator.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Implements the "AI Model -> Timeout -> Retry -> Circuit Breaker ->
 * Fallback" pattern from plan section 29/37, applied to this service's two
 * synchronous downstream calls (rag-service, mcp-gateway-service):
 *
 *  - slidingWindowSize(10) / failureRateThreshold(50): after 10 calls, if
 *    >=50% failed, the breaker OPENS and stops calling the downstream
 *    service for waitDurationInOpenState -- failing fast instead of
 *    piling up threads waiting on a service that's already struggling.
 *  - timeLimiter 3s: a single call is not allowed to hang the orchestrator
 *    request indefinitely.
 *
 * See AiOrchestratorService for what happens on OPEN/timeout for each
 * call: RAG failures degrade to an ungrounded answer (still useful);
 * MCP tool failures surface a clear "temporarily unavailable" message
 * rather than a raw 500.
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(15))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(3))
                        .build())
                .build());
    }
}
