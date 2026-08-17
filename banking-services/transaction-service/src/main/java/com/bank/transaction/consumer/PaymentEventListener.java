package com.bank.transaction.consumer;

import com.bank.transaction.application.TransactionApplicationService;
import com.bank.transaction.domain.TransactionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Section F3: "Listens to PaymentCompletedEvent -> auto-creates transaction
 * records." Only reacts to PAYMENT_COMPLETED; other payment lifecycle
 * events (INITIATED, FAILED) do not produce a ledger entry.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final TransactionApplicationService transactionApplicationService;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(TransactionApplicationService transactionApplicationService, ObjectMapper objectMapper) {
        this.transactionApplicationService = transactionApplicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "banking.payment.events", groupId = "transaction-service")
    public void onPaymentEvent(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            if (!"PAYMENT_COMPLETED".equals(event.path("eventType").asText())) {
                return;
            }
            JsonNode payload = event.path("payload");
            String paymentId = event.path("aggregateId").asText();

            transactionApplicationService.recordFromEvent(
                    payload.path("sourceAccountId").asText(),
                    paymentId,
                    "PAYMENT",
                    TransactionType.DEBIT,
                    new BigDecimal(payload.path("amount").asText()),
                    payload.path("currency").asText(),
                    "Payment " + payload.path("paymentReference").asText());
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
        }
    }
}
