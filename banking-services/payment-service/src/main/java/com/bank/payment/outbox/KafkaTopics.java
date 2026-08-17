package com.bank.payment.outbox;

public final class KafkaTopics {
    public static final String PAYMENT_EVENTS = "banking.payment.events";
    public static final String PAYMENT_EVENTS_RETRY = "banking.payment.events.retry";
    public static final String PAYMENT_EVENTS_DLT = "banking.payment.events.dlt";

    private KafkaTopics() {
    }
}
