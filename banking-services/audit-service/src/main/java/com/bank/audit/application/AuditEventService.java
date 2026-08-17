package com.bank.audit.application;

import com.bank.audit.domain.AuditEvent;
import com.bank.audit.domain.AuditEventRepository;
import com.bank.audit.domain.AuditResult;
import com.bank.audit.dto.AuditEventResponse;
import com.bank.audit.dto.RecordAuditEventRequest;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Section 34: "Never log passwords, JWT tokens, API keys, full card
 * numbers, sensitive PII." redact() strips any details key that matches a
 * known-sensitive name before the row is ever written, rather than trusting
 * every calling service to remember not to send them.
 */
@Service
public class AuditEventService {

    private static final List<String> SENSITIVE_KEYS = List.of(
            "password", "token", "jwt", "apikey", "api_key", "secret", "cardnumber",
            "card_number", "pan", "cvv", "ssn", "otp");

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEventResponse record(RecordAuditEventRequest request) {
        AuditEvent event = new AuditEvent(
                request.userId(), request.action(), request.resource(), request.result(),
                request.ipAddress(), MDC.get("correlationId"), redact(request.details()));
        return AuditEventResponse.from(auditEventRepository.save(event));
    }

    /**
     * Called by AuditEventKafkaConsumer — same logic as record() but accepts
     * raw fields from the Kafka message so the consumer doesn't construct a DTO.
     */
    @Transactional
    public void recordFromKafka(String userId, String action, String resource,
                                 AuditResult result, String serviceName,
                                 Map<String, Object> details) {
        Map<String, Object> enriched = details != null ? new HashMap<>(details) : new HashMap<>();
        enriched.put("sourceSvc", serviceName);
        AuditEvent event = new AuditEvent(
                userId, action, resource, result,
                null, MDC.get("correlationId"), redact(enriched));
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getForUser(String userId, Pageable pageable) {
        return auditEventRepository.findByUserIdOrderByTimestampDesc(userId, pageable).map(AuditEventResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getForResource(String resource, Pageable pageable) {
        return auditEventRepository.findByResourceOrderByTimestampDesc(resource, pageable).map(AuditEventResponse::from);
    }

    private String redact(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        Map<String, Object> safe = new HashMap<>();
        details.forEach((key, value) -> {
            String normalizedKey = key.toLowerCase().replace("_", "").replace("-", "");
            boolean sensitive = SENSITIVE_KEYS.stream()
                    .anyMatch(s -> normalizedKey.contains(s.replace("_", "")));
            safe.put(key, sensitive ? "***REDACTED***" : value);
        });
        return safe.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
