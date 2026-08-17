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
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(NotificationDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "banking.payment.events", groupId = "notification-service")
    public void onPaymentEvent(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String eventType = event.path("eventType").asText();
            if (!"PAYMENT_COMPLETED".equals(eventType) && !"PAYMENT_FAILED".equals(eventType)) {
                return;
            }
            JsonNode payload = event.path("payload");
            String eventId = event.path("eventId").asText();
            String subject = eventType.equals("PAYMENT_COMPLETED") ? "Payment successful" : "Payment failed";
            String body = String.format("Payment %s of %s %s: %s",
                    payload.path("paymentReference").asText(), payload.path("amount").asText(),
                    payload.path("currency").asText(), eventType);

            dispatcher.dispatch(eventId, payload.path("sourceAccountId").asText(),
                    NotificationChannel.SMS, subject, body);
        } catch (Exception e) {
            log.error("Failed to process payment event for notification: {}", e.getMessage(), e);
        }
    }
}
