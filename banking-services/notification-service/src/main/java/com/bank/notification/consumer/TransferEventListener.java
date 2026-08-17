package com.bank.notification.consumer;

import com.bank.notification.application.NotificationDispatcher;
import com.bank.notification.domain.NotificationChannel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public TransferEventListener(NotificationDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "banking.transfer.events", groupId = "notification-service")
    public void onTransferEvent(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String eventType = event.path("eventType").asText();
            if (!"TRANSFER_COMPLETED".equals(eventType) && !"TRANSFER_FAILED".equals(eventType)) {
                return;
            }
            JsonNode payload = event.path("payload");
            String eventId = event.path("eventId").asText();
            String subject = eventType.equals("TRANSFER_COMPLETED") ? "Transfer successful" : "Transfer failed";
            String body = String.format("Transfer of %s %s to %s: %s",
                    payload.path("amount").asText(), payload.path("currency").asText(),
                    payload.path("destinationAccountId").asText(), eventType);

            dispatcher.dispatch(eventId, payload.path("sourceAccountId").asText(),
                    NotificationChannel.PUSH, subject, body);
        } catch (Exception e) {
            log.error("Failed to process transfer event for notification: {}", e.getMessage(), e);
        }
    }
}
