package com.bank.audit;

import com.bank.audit.application.AuditEventService;
import com.bank.audit.domain.AuditResult;
import com.bank.audit.dto.AuditEventResponse;
import com.bank.audit.dto.RecordAuditEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AuditServiceApplication.class)
class AuditEventServiceTest {

    @Autowired
    private AuditEventService auditEventService;

    @Test
    void sensitiveFieldsAreRedactedBeforeStorage() {
        AuditEventResponse response = auditEventService.record(new RecordAuditEventRequest(
                "USER-1", "LOGIN", "auth", AuditResult.SUCCESS, "127.0.0.1",
                Map.of("password", "hunter2", "cardNumber", "4111111111111111", "channel", "web")));

        assertThat(response.details()).contains("***REDACTED***");
        assertThat(response.details()).doesNotContain("hunter2");
        assertThat(response.details()).doesNotContain("4111111111111111");
        assertThat(response.details()).contains("channel=web");
    }

    @Test
    void eventsAreQueryableByUser() {
        auditEventService.record(new RecordAuditEventRequest(
                "USER-2", "ACCOUNT_VIEW", "account-service", AuditResult.SUCCESS, null, null));

        var page = auditEventService.getForUser("USER-2", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).action()).isEqualTo("ACCOUNT_VIEW");
    }
}
