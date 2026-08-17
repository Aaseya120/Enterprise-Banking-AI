package com.bank.audit.controller;

import com.bank.audit.application.AuditEventService;
import com.bank.audit.dto.AuditEventResponse;
import com.bank.audit.dto.RecordAuditEventRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @PostMapping("/events")
    public ResponseEntity<AuditEventResponse> record(@Valid @RequestBody RecordAuditEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditEventService.record(request));
    }

    @GetMapping("/events/user/{userId}")
    public Page<AuditEventResponse> getForUser(@PathVariable String userId, Pageable pageable) {
        return auditEventService.getForUser(userId, pageable);
    }

    @GetMapping("/events/resource/{resource}")
    public Page<AuditEventResponse> getForResource(@PathVariable String resource, Pageable pageable) {
        return auditEventService.getForResource(resource, pageable);
    }
}
