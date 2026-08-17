-- notification-service schema, mirrors com.bank.notification.domain.Notification.
-- source_event_id is unique because NotificationDispatcher.dispatch() uses
-- existsBySourceEventId as its idempotent-consumer check against
-- at-least-once Kafka redelivery.
CREATE TABLE notifications (
    notification_id VARCHAR(36)  NOT NULL PRIMARY KEY,
    source_event_id VARCHAR(255) NOT NULL,
    recipient_ref   VARCHAR(255) NOT NULL,
    channel         VARCHAR(20)  NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            VARCHAR(1000) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_notifications_source_event_id UNIQUE (source_event_id)
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_ref, created_at DESC);
