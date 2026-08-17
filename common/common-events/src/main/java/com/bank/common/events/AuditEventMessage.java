package com.bank.common.events;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka message payload for the banking.audit.events topic.
 * Every service that performs a sensitive action publishes one of these;
 * audit-service's AuditEventKafkaConsumer persists them to the audit trail.
 *
 * <p>Fields map 1:1 to audit-service's RecordAuditEventRequest so the
 * consumer can reconstruct it without any transformation logic.
 *
 * <p>The "result" field uses String (not the AuditResult enum) so that
 * common-events remains free of audit-service's domain types — the consumer
 * converts it back to the enum on arrival.
 */
public record AuditEventMessage(
        String userId,
        String action,
        String resource,
        String result,          // "SUCCESS" | "FAILURE" | "BLOCKED"
        String serviceName,     // originating service, e.g. "payment-service"
        Instant occurredAt,
        Map<String, Object> details
) {
}
