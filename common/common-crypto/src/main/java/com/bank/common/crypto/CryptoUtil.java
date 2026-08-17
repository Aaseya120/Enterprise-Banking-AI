package com.bank.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Two independent, deliberately separate capabilities -- do not conflate
 * them, they solve different problems:
 *
 * <p><b>encrypt/decrypt</b> (AES-256-GCM): protects data AT REST. Use this
 * for a sensitive field you need to store and later read back in plaintext
 * (e.g. a national ID number) -- see customer-service's
 * {@code nationalIdEncrypted} field for the concrete example. GCM is an
 * authenticated mode: it detects tampering (a flipped ciphertext bit fails
 * to decrypt with an exception) as a side effect, but that is NOT the same
 * guarantee as HMAC below, which is for verifying who sent a message.
 *
 * <p><b>hmacSha256/verifyHmac</b>: protects a message IN TRANSIT between
 * two services that share a secret -- proves the message was produced by
 * someone holding that secret and wasn't altered en route, without
 * encrypting the message itself. Use this for a webhook-style callback
 * where the receiver must reject requests that didn't really come from
 * the expected sender -- see fraud-ai-service's callback to
 * transfer-service's {@code POST /transfers/{id}/fraud-decision}, which
 * would otherwise be callable by anyone who can reach the port.
 *
 * <p>Key handling: every method takes the key/secret as a caller-supplied
 * Base64 string sourced from configuration (see each consuming service's
 * {@code bank.crypto.*} properties). This is a demo-appropriate stand-in
 * for a real KMS/Vault-backed key -- the same pattern already used for the
 * gateway's JWT signing secret, and flagged the same way: never commit a
 * real key to source control.
 */
public final class CryptoUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtil() {
    }

    /**
     * Derives a 256-bit AES key from an arbitrary-length passphrase using
     * PBKDF2, so callers can configure a human-manageable secret string
     * rather than juggling raw key bytes. The salt is fixed and public
     * (not a secret itself) -- PBKDF2 here is a key-derivation
     * convenience, not password storage, so a per-value salt isn't needed.
     */
    public static String deriveAesKeyBase64(String passphrase) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    passphrase.toCharArray(),
                    "banking-ai-platform-crypto-salt".getBytes(StandardCharsets.UTF_8),
                    65536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Failed to derive AES key", e);
        }
    }

    /** Encrypts plaintext with AES-256-GCM. Returns Base64(IV || ciphertext || authTag). */
    public static String encrypt(String plaintext, String base64Key) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(base64Key), AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    /** Reverses encrypt(). Throws CryptoException if the key is wrong or the data was tampered with. */
    public static String decrypt(String base64CipherText, String base64Key) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64CipherText);
            if (combined.length < GCM_IV_LENGTH_BYTES) {
                throw new CryptoException("Ciphertext too short to contain an IV", null);
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(base64Key), AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Decryption failed -- wrong key or tampered ciphertext", e);
        }
    }

    /** Computes a hex-encoded HMAC-SHA256 signature of data using base64Secret. */
    public static String hmacSha256(String data, String base64Secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(base64Secret), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(signature);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("HMAC computation failed", e);
        }
    }

    /**
     * Constant-time comparison of a freshly-computed HMAC against a
     * caller-supplied one. Uses MessageDigest.isEqual rather than
     * String.equals/== specifically to avoid a timing side-channel that
     * could let an attacker guess the correct signature byte-by-byte.
     */
    public static boolean verifyHmac(String data, String base64Secret, String expectedHexSignature) {
        String actual = hmacSha256(data, base64Secret);
        return java.security.MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expectedHexSignature.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
