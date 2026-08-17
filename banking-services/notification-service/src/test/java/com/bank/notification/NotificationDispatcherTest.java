package com.bank.notification;

import com.bank.notification.application.NotificationDispatcher;
import com.bank.notification.domain.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NotificationServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"banking.payment.events", "banking.transfer.events"})
class NotificationDispatcherTest {

    @Autowired
    private NotificationDispatcher dispatcher;

    @Test
    void dispatchingTwiceWithSameSourceEventIdIsIdempotent() {
        dispatcher.dispatch("EVT-1", "ACC-1", NotificationChannel.SMS, "Subject", "Body");
        dispatcher.dispatch("EVT-1", "ACC-1", NotificationChannel.SMS, "Subject", "Body");

        var page = dispatcher.getForRecipient("ACC-1", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
