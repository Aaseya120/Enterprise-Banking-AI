package com.bank.common.crypto;

/** Unchecked wrapper around the JDK's checked crypto exceptions (GeneralSecurityException et al). */
public class CryptoException extends RuntimeException {
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
