package com.bank.payment.application;

import com.bank.common.events.AuditEventPublisher;
import com.bank.common.events.DomainEvent;
import com.bank.common.exception.BusinessException;
import com.bank.payment.client.FraudServiceClient;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentRepository;
import com.bank.payment.dto.InitiatePaymentRequest;
import com.bank.payment.dto.PaymentResponse;
import com.bank.payment.outbox.OutboxEvent;
import com.bank.payment.outbox.OutboxEventRepository;
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
 * Implements the payment lifecycle (plan section D) plus Idempotency-Key
 * deduplication (section 14): a retried request carrying an Idempotency-Key
 * that has already been processed returns the existing Payment instead of
 * creating a duplicate financial transaction. The Payment write and its
 * outbox event commit in the same local transaction (Transactional Outbox
 * pattern, same as transfer-service).
 *
 * <p>Fraud pre-check (gap filled): FraudServiceClient.evaluate() runs a
 * synchronous rule-based fraud check BEFORE the payment completes. On a
 * REJECT decision the payment is marked FAILED with a fraud reason and the
 * method returns without completing the transaction. If fraud-service is
 * unavailable the check is skipped (fail-open) with an audit note —
 * catastrophic fraud-service outages must not halt all payments; the async
 * fraud-ai saga is the deeper guard for transfers.
 *
 * <p>Audit emission (gap filled): every state transition publishes an
 * AuditEventMessage to banking.audit.events via AuditEventPublisher.
 */
@Service
@SuppressWarnings("null")
public class PaymentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplicationService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final AuditEventPublisher auditPublisher;
    private final FraudServiceClient fraudClient;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                      OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper,
                                      AuditEventPublisher auditPublisher,
                                      FraudServiceClient fraudClient) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.auditPublisher = auditPublisher;
        this.fraudClient = fraudClient;
    }

    @Transactional
    public PaymentResponse initiatePayment(String idempotencyKey, InitiatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw BusinessException.validation("Idempotency-Key header is required");
        }

        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(PaymentResponse::from)
                .orElseGet(() -> createAndProcess(idempotencyKey, request));
    }

    private PaymentResponse createAndProcess(String idempotencyKey, InitiatePaymentRequest request) {
        Payment payment = new Payment(
                idempotencyKey, request.paymentType(), request.sourceAccountId(), request.destinationRef(),
                request.destinationBank(), request.destinationIfsc(), request.amount(), request.currency(),
                request.remarks());

        publishEvent(payment, "PAYMENT_INITIATED");
        auditPublisher.publish(
                MDC.get("userId"), "PAYMENT_INITIATED",
                "Payment/" + payment.getPaymentReference(),
                true, "payment-service",
                Map.of("sourceAccountId", payment.getSourceAccountId(),
                        "amount", payment.getAmount(),
                        "currency", payment.getCurrency()));

        payment.authorize();
        payment.markProcessing();

        // Fraud pre-check before final completion.
        FraudServiceClient.FraudDecision fraud = fraudClient.evaluate(
                payment.getSourceAccountId(), payment.getAmount(), payment.getCurrency());

        if (!fraud.allowed()) {
            payment.markFailed("Fraud check rejected: " + fraud.reason());
            publishEvent(payment, "PAYMENT_FAILED");
            auditPublisher.publish(
                    MDC.get("userId"), "PAYMENT_BLOCKED_FRAUD",
                    "Payment/" + payment.getPaymentReference(),
                    false, "payment-service",
                    Map.of("fraudReason", fraud.reason()));
            payment = paymentRepository.save(payment);
            return PaymentResponse.from(payment);
        }

        if (fraud.skipped()) {
            log.warn("[fraud-check] Skipped for payment {} — fraud-service unavailable",
                    payment.getPaymentReference());
            auditPublisher.publish(
                    MDC.get("userId"), "FRAUD_CHECK_SKIPPED",
                    "Payment/" + payment.getPaymentReference(),
                    true, "payment-service",
                    Map.of("reason", "fraud-service unreachable"));
        }

        payment.markCompleted();
        publishEvent(payment, "PAYMENT_COMPLETED");
        auditPublisher.publish(
                MDC.get("userId"), "PAYMENT_COMPLETED",
                "Payment/" + payment.getPaymentReference(),
                true, "payment-service",
                Map.of("sourceAccountId", payment.getSourceAccountId(),
                        "amount", payment.getAmount()));

        payment = paymentRepository.save(payment);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        return PaymentResponse.from(findOrThrow(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> BusinessException.notFound("Payment not found: " + paymentReference));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PaymentResponse> getPaymentsForAccount(
            String accountId, org.springframework.data.domain.Pageable pageable) {
        return paymentRepository.findBySourceAccountId(accountId, pageable).map(PaymentResponse::from);
    }

    @Transactional
    public PaymentResponse cancelPayment(String paymentId) {
        Payment payment = findOrThrow(paymentId);
        if (payment.getStatus().name().equals("COMPLETED")) {
            throw BusinessException.ruleViolation("Cannot cancel a completed payment; use /reverse instead");
        }
        payment.markFailed("Cancelled by request");
        publishEvent(payment, "PAYMENT_FAILED");
        auditPublisher.publish(
                MDC.get("userId"), "PAYMENT_CANCELLED",
                "Payment/" + payment.getPaymentId(),
                true, "payment-service",
                Map.of("paymentId", payment.getPaymentId()));
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse reversePayment(String paymentId) {
        Payment payment = findOrThrow(paymentId);
        payment.markReversed();
        publishEvent(payment, "PAYMENT_REVERSED");
        auditPublisher.publish(
                MDC.get("userId"), "PAYMENT_REVERSED",
                "Payment/" + payment.getPaymentId(),
                true, "payment-service",
                Map.of("paymentId", payment.getPaymentId()));
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    private Payment findOrThrow(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.notFound("Payment not found: " + paymentId));
    }

    private void publishEvent(Payment payment, String eventType) {
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                eventType,
                payment.getPaymentId() != null ? payment.getPaymentId() : payment.getPaymentReference(),
                Instant.now(),
                1,
                MDC.get("correlationId"),
                null,
                Map.of(
                        "paymentReference", payment.getPaymentReference(),
                        "sourceAccountId", payment.getSourceAccountId(),
                        "destinationRef", payment.getDestinationRef(),
                        "amount", payment.getAmount(),
                        "currency", payment.getCurrency(),
                        "status", payment.getStatus().name()
                )
        );
        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            String aggregateId = payment.getPaymentId() != null ? payment.getPaymentId() : payment.getPaymentReference();
            outboxEventRepository.save(new OutboxEvent(aggregateId, "Payment", eventType, payloadJson));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}
