package com.bank.transfer.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls for PENDING outbox rows and publishes them to Kafka, then marks
 * them PUBLISHED. This decouples "commit the business change" from
 * "publish the event" -- if Kafka is briefly unavailable, the row just
 * stays PENDING and is retried on the next poll instead of the whole
 * request failing (section 12/23). After 5 failed attempts a row moves to
 * DEAD_LETTER for manual/ops investigation rather than retrying forever.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           @Qualifier("kafkaStringTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${bank.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(KafkaTopics.TRANSFER_EVENTS, event.getAggregateId(), event.getPayload()).get();
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
