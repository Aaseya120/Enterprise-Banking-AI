package com.bank.transfer.controller;

import com.bank.common.crypto.CryptoUtil;
import com.bank.common.exception.BusinessException;
import com.bank.transfer.application.TransferApplicationService;
import com.bank.transfer.dto.InitiateTransferRequest;
import com.bank.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferApplicationService transferApplicationService;
    private final String hmacSecret;

    public TransferController(TransferApplicationService transferApplicationService,
                               @Value("${bank.crypto.fraud-decision-hmac-secret}") String hmacSecretPassphrase) {
        this.transferApplicationService = transferApplicationService;
        this.hmacSecret = Base64.getEncoder().encodeToString(
                hmacSecretPassphrase.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping
    public ResponseEntity<TransferResponse> initiate(@Valid @RequestBody InitiateTransferRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transferApplicationService.initiateTransfer(request));
    }

    @GetMapping("/{transferId}")
    public TransferResponse get(@PathVariable String transferId) {
        return transferApplicationService.getTransfer(transferId);
    }

    /**
     * Callback used by fraud-ai-service once it has scored the transfer
     * (stands in for a second Kafka consumer in this scaffold -- see
     * TransferApplicationService.applyFraudDecision javadoc).
     *
     * Without X-Signature verification, this endpoint would let anyone who
     * can reach this service's port approve their own transfer regardless
     * of what fraud-ai-service actually decided -- a much bigger hole than
     * a missing role check, since it bypasses the fraud review entirely.
     * The signature is HMAC-SHA256 over a canonical string built from the
     * request (see fraud-ai-service's TransferServiceClient.canonicalize,
     * which MUST stay byte-for-byte identical to the reconstruction below
     * -- the two services share only the secret and this string format,
     * not code, so they can drift silently if either side changes without
     * the other. A shared common-crypto module reduces but doesn't
     * eliminate that risk, since the canonical string shape itself still
     * lives in two places.)
     */
    @PostMapping("/{transferId}/fraud-decision")
    public TransferResponse applyFraudDecision(@PathVariable String transferId,
                                                @RequestParam boolean approved,
                                                @RequestHeader(value = "X-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            throw BusinessException.forbidden("Missing X-Signature header on fraud-decision callback");
        }
        String canonicalPayload = "transferId=" + transferId + ";approved=" + approved;
        if (!CryptoUtil.verifyHmac(canonicalPayload, hmacSecret, signature)) {
            throw BusinessException.forbidden("Invalid signature on fraud-decision callback");
        }
        return transferApplicationService.applyFraudDecision(transferId, approved);
    }
}
