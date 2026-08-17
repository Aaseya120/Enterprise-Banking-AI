package com.bank.transfer;

import com.bank.transfer.application.TransferApplicationService;
import com.bank.transfer.dto.InitiateTransferRequest;
import com.bank.transfer.dto.TransferResponse;
import com.bank.transfer.outbox.OutboxEventRepository;
import com.bank.transfer.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TransferServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"banking.transfer.events", "banking.audit.events"})
class TransferApplicationServiceTest {

    @Autowired
    private TransferApplicationService transferApplicationService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void initiatingTransferWritesOutboxEventInSameTransaction() {
        TransferResponse response = transferApplicationService.initiateTransfer(
                new InitiateTransferRequest("ACC-1", "ACC-2", new BigDecimal("100.00"), "USD"));

        assertThat(response.status().name()).isEqualTo("PENDING_FRAUD_REVIEW");

        long pendingOutboxRows = outboxEventRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(response.transferId()))
                .filter(e -> e.getStatus() == OutboxStatus.PENDING || e.getStatus() == OutboxStatus.PUBLISHED)
                .count();
        assertThat(pendingOutboxRows).isEqualTo(1);
    }

    @Test
    void rejectsTransferToSameAccount() {
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                transferApplicationService.initiateTransfer(
                        new InitiateTransferRequest("ACC-1", "ACC-1", new BigDecimal("50.00"), "USD")));
    }
}
