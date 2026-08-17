package com.bank.notification.controller;

import com.bank.notification.application.NotificationDispatcher;
import com.bank.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationDispatcher dispatcher;

    public NotificationController(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @GetMapping("/recipient/{recipientRef}")
    public Page<Notification> getForRecipient(@PathVariable String recipientRef, Pageable pageable) {
        return dispatcher.getForRecipient(recipientRef, pageable);
    }
}
