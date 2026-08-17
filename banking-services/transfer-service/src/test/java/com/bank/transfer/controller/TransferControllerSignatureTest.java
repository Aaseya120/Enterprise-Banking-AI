package com.bank.transfer.controller;

import com.bank.common.crypto.CryptoUtil;
import com.bank.transfer.TransferServiceApplication;
import com.bank.transfer.application.TransferApplicationService;
import com.bank.transfer.dto.InitiateTransferRequest;
import com.bank.transfer.dto.TransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the HMAC signature check on POST /fraud-decision (plan
 * requirement: HMAC-signed inter-service callback). The shared secret
 * here matches src/test/resources/application.yml's
 * bank.crypto.fraud-decision-hmac-secret.
 */
@SpringBootTest(classes = TransferServiceApplication.class)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"banking.transfer.events", "banking.audit.events"})
class TransferControllerSignatureTest {

    private static final String TEST_SECRET_PASSPHRASE = "test-only-shared-secret-not-used-outside-this-test-run";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransferApplicationService transferApplicationService;

    @Test
    void rejectsCallbackWithNoSignatureHeader() throws Exception {
        String transferId = initiateTransfer();

        mockMvc.perform(post("/api/v1/transfers/{id}/fraud-decision", transferId)
                        .param("approved", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsCallbackWithAnIncorrectSignature() throws Exception {
        String transferId = initiateTransfer();

        mockMvc.perform(post("/api/v1/transfers/{id}/fraud-decision", transferId)
                        .param("approved", "true")
                        .header("X-Signature", "0000000000000000000000000000000000000000000000000000000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsCallbackWithAValidSignature() throws Exception {
        String transferId = initiateTransfer();

        String base64Secret = Base64.getEncoder().encodeToString(
                TEST_SECRET_PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        String canonicalPayload = "transferId=" + transferId + ";approved=true";
        String signature = CryptoUtil.hmacSha256(canonicalPayload, base64Secret);

        mockMvc.perform(post("/api/v1/transfers/{id}/fraud-decision", transferId)
                        .param("approved", "true")
                        .header("X-Signature", signature))
                .andExpect(status().isOk());
    }

    private String initiateTransfer() {
        TransferResponse response = transferApplicationService.initiateTransfer(
                new InitiateTransferRequest("ACC-SIG-1", "ACC-SIG-2", new BigDecimal("25.00"), "USD"));
        return response.transferId();
    }
}
