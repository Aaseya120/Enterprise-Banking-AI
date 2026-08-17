package com.bank.ai.fraud.consumer;

import com.bank.ai.fraud.client.TransferServiceClient;
import com.bank.ai.fraud.model.RiskScore;
import com.bank.ai.fraud.service.RiskScoringService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements the Fraud AI flow (section 25/33):
 * Kafka -> Feature extraction -> Risk model -> Risk score -> Decision.
 * Only reacts to TRANSFER_INITIATED events (the saga's fraud-review step);
 * TRANSFER_COMPLETED/FAILED events it produced itself via the callback are
 * ignored to avoid a feedback loop.
 */
@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);

    private final RiskScoringService riskScoringService;
    private final TransferServiceClient transferServiceClient;
    private final ObjectMapper objectMapper;

    public TransferEventListener(RiskScoringService riskScoringService,
                                  TransferServiceClient transferServiceClient,
                                  ObjectMapper objectMapper) {
        this.riskScoringService = riskScoringService;
        this.transferServiceClient = transferServiceClient;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "banking.transfer.events", groupId = "fraud-ai-service")
    public void onTransferEvent(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String eventType = event.path("eventType").asText();
            if (!"TRANSFER_INITIATED".equals(eventType)) {
                return;
            }

            String transferId = event.path("aggregateId").asText();
            Map<String, Object> payload = toMap(event.path("payload"));

            RiskScore riskScore = riskScoringService.score(transferId, payload);
            log.info("Scored transfer {} -> {} ({}): {}",
                    transferId, riskScore.level(), riskScore.score(), riskScore.reason());

            transferServiceClient.submitFraudDecision(transferId, riskScore.isApproved());
        } catch (Exception e) {
            // A production consumer would route this to a .retry / .dlt topic
            // (section 13) instead of just logging; kept simple here.
            log.error("Failed to process transfer event: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> toMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            JsonNode value = node.get(field);
            map.put(field, value.isNumber() ? value.numberValue() : value.asText());
        }
        return map;
    }
}
