package com.bank.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Fire-and-forget audit event publisher. Sends to banking.audit.events.
 *
 * <p>Design: publishing failure is deliberately swallowed after logging —
 * the audit trail is a secondary concern; the calling service's business
 * transaction must not fail because audit-service or Kafka is unavailable.
 * The audit-service consumer is idempotent on (userId, action, resource,
 * occurredAt) so safe retries by Kafka's producer retry mechanism are fine.
 */
@Component
@SuppressWarnings("null")
public class AuditEventPublisher {

    static final String TOPIC = "banking.audit.events";
    private static final Logger log = LoggerFactory.getLogger(AuditEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuditEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * @param userId      authenticated user performing the action (or "system")
     * @param action      human-readable verb, e.g. "PAYMENT_INITIATED"
     * @param resource    resource identifier, e.g. "Payment/pay-123"
     * @param success     true → "SUCCESS", false → "FAILURE"
     * @param serviceName originating service name, e.g. "payment-service"
     * @param details     additional key-value context (will be redacted by audit-service)
     */
    public void publish(String userId, String action, String resource,
                        boolean success, String serviceName, Map<String, Object> details) {
        AuditEventMessage message = new AuditEventMessage(
                userId != null ? userId : "system",
                action,
                resource,
                success ? "SUCCESS" : "FAILURE",
                serviceName,
                Instant.now(),
                details
        );
        try {
            kafkaTemplate.send(TOPIC, resource, message);
        } catch (Exception ex) {
            // Audit publishing must never fail the business transaction.
            log.warn("[audit] Failed to publish audit event action={} resource={} correlationId={}: {}",
                    action, resource, MDC.get("correlationId"), ex.getMessage());
        }
    }
}
