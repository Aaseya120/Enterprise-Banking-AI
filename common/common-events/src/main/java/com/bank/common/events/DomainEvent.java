package com.bank.common.events;

import java.time.Instant;
import java.util.Map;

/**
 * Envelope for every event published to banking.*.events topics
 * (architecture plan section 22). Producers should publish this via the
 * transactional outbox pattern rather than directly from request threads.
 */
public record DomainEvent(
        String eventId,
        String eventType,
        String aggregateId,
        Instant timestamp,
        int version,
        String correlationId,
        String traceId,
        Map<String, Object> payload
) {
}
