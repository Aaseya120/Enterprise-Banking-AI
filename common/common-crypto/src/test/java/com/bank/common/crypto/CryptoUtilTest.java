package com.bank.common.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoUtilTest {

    private final String key = CryptoUtil.deriveAesKeyBase64("test-passphrase-not-for-production");
    private final String hmacSecret = Base64.getEncoder().encodeToString("test-hmac-secret".repeat(4).getBytes());

    @Test
    void encryptDecryptRoundTripsToTheOriginalPlaintext() {
        String plaintext = "123-45-6789";
        String ciphertext = CryptoUtil.encrypt(plaintext, key);

        assertThat(ciphertext).isNotEqualTo(plaintext);
        assertThat(CryptoUtil.decrypt(ciphertext, key)).isEqualTo(plaintext);
    }

    @Test
    void sameInputEncryptsDifferentlyEachTimeBecauseOfRandomIv() {
        String plaintext = "123-45-6789";
        String first = CryptoUtil.encrypt(plaintext, key);
        String second = CryptoUtil.encrypt(plaintext, key);

        assertThat(first).isNotEqualTo(second);
        assertThat(CryptoUtil.decrypt(first, key)).isEqualTo(plaintext);
        assertThat(CryptoUtil.decrypt(second, key)).isEqualTo(plaintext);
    }

    @Test
    void decryptingWithTheWrongKeyFails() {
        String ciphertext = CryptoUtil.encrypt("sensitive-value", key);
        String wrongKey = CryptoUtil.deriveAesKeyBase64("a-completely-different-passphrase");

        assertThatThrownBy(() -> CryptoUtil.decrypt(ciphertext, wrongKey))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void tamperedCiphertextFailsToDecryptInsteadOfReturningCorruptedData() {
        String ciphertext = CryptoUtil.encrypt("sensitive-value", key);
        byte[] raw = Base64.getDecoder().decode(ciphertext);
        raw[raw.length - 1] ^= 0x01; // flip a bit in the auth tag
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> CryptoUtil.decrypt(tampered, key))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void hmacVerificationAcceptsAValidSignatureAndRejectsAnInvalidOne() {
        String payload = "transferId=T-1;approved=true";
        String signature = CryptoUtil.hmacSha256(payload, hmacSecret);

        assertThat(CryptoUtil.verifyHmac(payload, hmacSecret, signature)).isTrue();
        assertThat(CryptoUtil.verifyHmac(payload, hmacSecret, "deadbeef")).isFalse();
        assertThat(CryptoUtil.verifyHmac("tampered-payload", hmacSecret, signature)).isFalse();
    }
}
