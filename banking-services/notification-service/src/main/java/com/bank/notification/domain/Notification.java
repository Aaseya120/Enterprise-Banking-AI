package com.bank.notification.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = "sourceEventId"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String notificationId;

    /** The upstream event id (payment/transfer id) -- used for idempotent consumption. */
    @Column(nullable = false, unique = true)
    private String sourceEventId;

    @Column(nullable = false)
    private String recipientRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public Notification(String sourceEventId, String recipientRef, NotificationChannel channel,
                         String subject, String body) {
        this.sourceEventId = sourceEventId;
        this.recipientRef = recipientRef;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.SENT; // demo: dispatched synchronously, always "succeeds"
        this.createdAt = Instant.now();
    }

    public String getNotificationId() { return notificationId; }
    public String getSourceEventId() { return sourceEventId; }
    public String getRecipientRef() { return recipientRef; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
