package com.bank.notification.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    boolean existsBySourceEventId(String sourceEventId);
    Page<Notification> findByRecipientRefOrderByCreatedAtDesc(String recipientRef, Pageable pageable);
}
