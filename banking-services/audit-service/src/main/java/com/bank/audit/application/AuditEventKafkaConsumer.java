package com.bank.audit.application;

import com.bank.audit.domain.AuditResult;
import com.bank.common.events.AuditEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes from banking.audit.events and persists each message via the
 * existing AuditEventService (which handles redaction). This is a second
 * ingest path alongside POST /api/v1/audit/events — both write to the
 * same audit_events table, so REST-based callers continue to work unchanged.
 *
 * <p>Idempotency: Kafka "at-least-once" delivery means duplicates are
 * possible. audit_events has no unique constraint on (userId, action,
 * resource, timestamp) so true duplicates will produce two rows — an
 * acceptable trade-off for an audit trail where false negatives are worse
 * than false positives. A dedup index can be added if the operational
 * requirement changes.
 */
@Component
public class AuditEventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventKafkaConsumer.class);

    private final AuditEventService auditEventService;

    public AuditEventKafkaConsumer(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @KafkaListener(topics = "banking.audit.events", groupId = "audit-service-consumer")
    public void onAuditEvent(AuditEventMessage message) {
        try {
            AuditResult result = parseResult(message.result());
            auditEventService.recordFromKafka(
                    message.userId(),
                    message.action(),
                    message.resource(),
                    result,
                    message.serviceName(),
                    message.details()
            );
        } catch (Exception ex) {
            // Log and continue — do not re-throw; we don't want the consumer
            // offset to be stuck retrying a single malformed message forever.
            // A dead-letter topic would be the production answer.
            log.error("[audit] Failed to persist audit event from Kafka: action={} resource={} error={}",
                    message.action(), message.resource(), ex.getMessage(), ex);
        }
    }

    private AuditResult parseResult(String resultString) {
        try {
            return AuditResult.valueOf(resultString);
        } catch (IllegalArgumentException e) {
            log.warn("[audit] Unknown AuditResult value '{}', defaulting to SUCCESS", resultString);
            return AuditResult.SUCCESS;
        }
    }
}
