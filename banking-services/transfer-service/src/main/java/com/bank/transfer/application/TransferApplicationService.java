package com.bank.transfer.application;

import com.bank.common.events.AuditEventPublisher;
import com.bank.common.events.DomainEvent;
import com.bank.common.exception.BusinessException;
import com.bank.transfer.client.FraudServiceClient;
import com.bank.transfer.domain.Transfer;
import com.bank.transfer.domain.TransferRepository;
import com.bank.transfer.dto.InitiateTransferRequest;
import com.bank.transfer.dto.TransferResponse;
import com.bank.transfer.outbox.OutboxEvent;
import com.bank.transfer.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the "INITIATED" step of the transfer saga (section 10/11/24).
 * The local ACID transaction covers exactly two writes: the Transfer row
 * and its outbox event — never a distributed transaction across services.
 *
 * <p>Fraud pre-check (gap filled): FraudServiceClient.evaluate() runs a
 * synchronous rule-based check before a transfer is accepted. A REJECT
 * decision fails the transfer immediately (before it enters
 * PENDING_FRAUD_REVIEW). If fraud-service is unreachable the check is
 * skipped (fail-open) — the async fraud-ai-service saga is still a second
 * line of defence.
 *
 * <p>Audit emission (gap filled): every state transition publishes an
 * AuditEventMessage to banking.audit.events.
 */
@Service
@SuppressWarnings("null")
public class TransferApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TransferApplicationService.class);

    private final TransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final AuditEventPublisher auditPublisher;
    private final FraudServiceClient fraudClient;

    public TransferApplicationService(TransferRepository transferRepository,
                                       OutboxEventRepository outboxEventRepository,
                                       ObjectMapper objectMapper,
                                       AuditEventPublisher auditPublisher,
                                       FraudServiceClient fraudClient) {
        this.transferRepository = transferRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.auditPublisher = auditPublisher;
        this.fraudClient = fraudClient;
    }

    @Transactional
    public TransferResponse initiateTransfer(InitiateTransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw BusinessException.validation("Source and destination accounts must differ");
        }

        // Synchronous rule-based fraud pre-check (business-rule engine).
        FraudServiceClient.FraudDecision fraud = fraudClient.evaluate(
                request.sourceAccountId(), request.amount(), request.currency());

        if (!fraud.allowed()) {
            auditPublisher.publish(
                    MDC.get("userId"), "TRANSFER_BLOCKED_FRAUD",
                    "Transfer/pre-check",
                    false, "transfer-service",
                    Map.of("sourceAccountId", request.sourceAccountId(),
                            "amount", request.amount(),
                            "fraudReason", fraud.reason()));
            throw BusinessException.ruleViolation("Transfer rejected by fraud check: " + fraud.reason());
        }

        if (fraud.skipped()) {
            log.warn("[fraud-check] Skipped for transfer from {} — fraud-service unavailable",
                    request.sourceAccountId());
            auditPublisher.publish(
                    MDC.get("userId"), "FRAUD_CHECK_SKIPPED",
                    "Transfer/pre-check",
                    true, "transfer-service",
                    Map.of("sourceAccountId", request.sourceAccountId(),
                            "reason", "fraud-service unreachable"));
        }

        Transfer transfer = new Transfer(
                request.sourceAccountId(), request.destinationAccountId(), request.amount(), request.currency());
        transfer.markPendingFraudReview();
        transfer = transferRepository.save(transfer);

        publishEvent(transfer, "TRANSFER_INITIATED");
        auditPublisher.publish(
                MDC.get("userId"), "TRANSFER_INITIATED",
                "Transfer/" + transfer.getTransferId(),
                true, "transfer-service",
                Map.of("sourceAccountId", transfer.getSourceAccountId(),
                        "destinationAccountId", transfer.getDestinationAccountId(),
                        "amount", transfer.getAmount(),
                        "currency", transfer.getCurrency()));

        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(String transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> BusinessException.notFound("Transfer not found: " + transferId));
        return TransferResponse.from(transfer);
    }

    /**
     * Called by the fraud-decision consumer to move the saga forward or
     * trigger compensation.
     */
    @Transactional
    public TransferResponse applyFraudDecision(String transferId, boolean approved) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> BusinessException.notFound("Transfer not found: " + transferId));

        if (approved) {
            transfer.markCompleted();
            publishEvent(transfer, "TRANSFER_COMPLETED");
            auditPublisher.publish(
                    "system", "TRANSFER_COMPLETED",
                    "Transfer/" + transferId,
                    true, "transfer-service",
                    Map.of("transferId", transferId, "decisionBy", "fraud-ai-service"));
        } else {
            transfer.markFailed();
            publishEvent(transfer, "TRANSFER_FAILED");
            auditPublisher.publish(
                    "system", "TRANSFER_FAILED",
                    "Transfer/" + transferId,
                    false, "transfer-service",
                    Map.of("transferId", transferId, "decisionBy", "fraud-ai-service"));
            // Compensation (section 11/24): since this scaffold's account-service
            // is not yet wired to actually debit on TRANSFER_INITIATED, there is
            // nothing to reverse here. In a full deployment this is where a
            // ReverseDebit compensation event would be published.
        }
        return TransferResponse.from(transferRepository.save(transfer));
    }

    private void publishEvent(Transfer transfer, String eventType) {
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                eventType,
                transfer.getTransferId(),
                Instant.now(),
                1,
                MDC.get("correlationId"),
                null,
                Map.of(
                        "sourceAccountId", transfer.getSourceAccountId(),
                        "destinationAccountId", transfer.getDestinationAccountId(),
                        "amount", transfer.getAmount(),
                        "currency", transfer.getCurrency(),
                        "status", transfer.getStatus().name()
                )
        );
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxEventRepository.save(new OutboxEvent(
                    transfer.getTransferId(), "Transfer", eventType, payload));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}
