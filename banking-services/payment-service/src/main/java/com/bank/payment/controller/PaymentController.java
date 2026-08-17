package com.bank.payment.controller;

import com.bank.payment.application.PaymentApplicationService;
import com.bank.payment.dto.InitiatePaymentRequest;
import com.bank.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> initiate(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentApplicationService.initiatePayment(idempotencyKey, request));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable String paymentId) {
        return paymentApplicationService.getPayment(paymentId);
    }

    @GetMapping("/reference/{reference}")
    public PaymentResponse getByReference(@PathVariable String reference) {
        return paymentApplicationService.getByReference(reference);
    }

    @GetMapping("/account/{accountId}")
    public Page<PaymentResponse> getForAccount(@PathVariable String accountId, Pageable pageable) {
        return paymentApplicationService.getPaymentsForAccount(accountId, pageable);
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentResponse cancel(@PathVariable String paymentId) {
        return paymentApplicationService.cancelPayment(paymentId);
    }

    @PostMapping("/{paymentId}/reverse")
    public PaymentResponse reverse(@PathVariable String paymentId) {
        return paymentApplicationService.reversePayment(paymentId);
    }
}
