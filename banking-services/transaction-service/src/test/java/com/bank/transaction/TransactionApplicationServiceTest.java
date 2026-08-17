package com.bank.transaction;

import com.bank.transaction.application.TransactionApplicationService;
import com.bank.transaction.domain.TransactionType;
import com.bank.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TransactionServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"banking.payment.events", "banking.transfer.events"})
class TransactionApplicationServiceTest {

    @Autowired
    private TransactionApplicationService transactionApplicationService;

    @Test
    void recordingTwiceWithSameReferenceIdIsIdempotent() {
        transactionApplicationService.recordFromEvent(
                "ACC-1", "REF-1", "PAYMENT", TransactionType.DEBIT, new BigDecimal("100.00"), "USD", "test");
        transactionApplicationService.recordFromEvent(
                "ACC-1", "REF-1", "PAYMENT", TransactionType.DEBIT, new BigDecimal("100.00"), "USD", "test");

        var statement = transactionApplicationService.getStatement("ACC-1", PageRequest.of(0, 10));
        assertThat(statement.getTotalElements()).isEqualTo(1);
    }

    @Test
    void statementReturnsRecordedTransaction() {
        transactionApplicationService.recordFromEvent(
                "ACC-2", "REF-2", "TRANSFER", TransactionType.CREDIT, new BigDecimal("250.00"), "USD", "transfer in");

        TransactionResponse response = transactionApplicationService.getByReferenceId("REF-2");
        assertThat(response.accountId()).isEqualTo("ACC-2");
        assertThat(response.transactionType()).isEqualTo(TransactionType.CREDIT);
    }
}
