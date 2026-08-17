package com.bank.notification.application;

import com.bank.notification.domain.Notification;
import com.bank.notification.domain.NotificationChannel;
import com.bank.notification.domain.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent on sourceEventId, same pattern as transaction-service's
 * ledger writer -- an at-least-once Kafka redelivery never sends the same
 * notification twice. dispatch() currently just persists a Notification
 * row with status SENT; swap that assignment (in the Notification
 * constructor) for a real call to an SMS/email/push provider (Twilio, SES,
 * FCM, ...) without changing the consumer or idempotency logic.
 */
@Service
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;

    public NotificationDispatcher(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void dispatch(String sourceEventId, String recipientRef, NotificationChannel channel,
                          String subject, String body) {
        if (notificationRepository.existsBySourceEventId(sourceEventId)) {
            return; // already notified -- idempotent no-op
        }
        notificationRepository.save(new Notification(sourceEventId, recipientRef, channel, subject, body));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getForRecipient(String recipientRef, Pageable pageable) {
        return notificationRepository.findByRecipientRefOrderByCreatedAtDesc(recipientRef, pageable);
    }
}
