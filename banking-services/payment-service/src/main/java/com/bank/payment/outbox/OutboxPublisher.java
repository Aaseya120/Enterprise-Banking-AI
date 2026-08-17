package com.bank.payment.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${bank.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, event.getAggregateId(), event.getPayload()).get();
                event.markPublished();
                log.info("Published outbox event {} ({}) for aggregate {}",
                        event.getId(), event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                event.markFailed();
                log.warn("Failed to publish outbox event {} (attempt {}): {}",
                        event.getId(), event.getRetryCount(), e.getMessage());
            }
            outboxEventRepository.save(event);
        }
    }
}
