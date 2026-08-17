package com.bank.payment;

import com.bank.payment.application.PaymentApplicationService;
import com.bank.payment.domain.PaymentType;
import com.bank.payment.dto.InitiatePaymentRequest;
import com.bank.payment.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PaymentServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"banking.payment.events", "banking.audit.events"})
class PaymentApplicationServiceTest {

    @Autowired
    private PaymentApplicationService paymentApplicationService;

    @Test
    void duplicateIdempotencyKeyReturnsSamePaymentInsteadOfCreatingANewOne() {
        String idempotencyKey = UUID.randomUUID().toString();
        InitiatePaymentRequest request = new InitiatePaymentRequest(
                "ACC-1", PaymentType.UPI, "ACC-2", null, null, new BigDecimal("100.00"), "USD", "test");

        PaymentResponse first = paymentApplicationService.initiatePayment(idempotencyKey, request);
        PaymentResponse second = paymentApplicationService.initiatePayment(idempotencyKey, request);

        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(second.paymentReference()).isEqualTo(first.paymentReference());
    }

    @Test
    void paymentCompletesSynchronouslyInThisScaffold() {
        InitiatePaymentRequest request = new InitiatePaymentRequest(
                "ACC-1", PaymentType.NEFT, "ACC-3", "HDFC", "HDFC0001", new BigDecimal("500.00"), "USD", null);

        PaymentResponse response = paymentApplicationService.initiatePayment(UUID.randomUUID().toString(), request);

        assertThat(response.status().name()).isEqualTo("COMPLETED");
        assertThat(response.paymentReference()).startsWith("PAY-");
    }
}
