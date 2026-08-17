package com.bank.transfer.outbox;

/**
 * Topic naming follows architecture plan section 13/22:
 * banking.<domain>.events, with .retry / .dlt siblings maintained by the
 * Kafka admin/consumer config (not created here -- see application.yml's
 * spring.kafka.consumer / a KafkaAdmin bean in a full deployment).
 */
public final class KafkaTopics {
    public static final String TRANSFER_EVENTS = "banking.transfer.events";
    public static final String TRANSFER_EVENTS_RETRY = "banking.transfer.events.retry";
    public static final String TRANSFER_EVENTS_DLT = "banking.transfer.events.dlt";

    private KafkaTopics() {
    }
}
