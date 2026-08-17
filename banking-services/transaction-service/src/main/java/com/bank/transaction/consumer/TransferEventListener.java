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
 * Mirrors PaymentEventListener for TRANSFER_COMPLETED: writes a DEBIT entry
 * for the source account and a CREDIT entry for the destination account,
 * both idempotent on referenceId (transaction-service's
 * existsByReferenceId check), so redelivery is harmless.
 */
@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);

    private final TransactionApplicationService transactionApplicationService;
    private final ObjectMapper objectMapper;

    public TransferEventListener(TransactionApplicationService transactionApplicationService, ObjectMapper objectMapper) {
        this.transactionApplicationService = transactionApplicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "banking.transfer.events", groupId = "transaction-service")
    public void onTransferEvent(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            if (!"TRANSFER_COMPLETED".equals(event.path("eventType").asText())) {
                return;
            }
            JsonNode payload = event.path("payload");
            String transferId = event.path("aggregateId").asText();
            BigDecimal amount = new BigDecimal(payload.path("amount").asText());
            String currency = payload.path("currency").asText();

            transactionApplicationService.recordFromEvent(
                    payload.path("sourceAccountId").asText(), transferId + "-DEBIT", "TRANSFER",
                    TransactionType.DEBIT, amount, currency, "Transfer out " + transferId);

            transactionApplicationService.recordFromEvent(
                    payload.path("destinationAccountId").asText(), transferId + "-CREDIT", "TRANSFER",
                    TransactionType.CREDIT, amount, currency, "Transfer in " + transferId);
        } catch (Exception e) {
            log.error("Failed to process transfer event: {}", e.getMessage(), e);
        }
    }
}
