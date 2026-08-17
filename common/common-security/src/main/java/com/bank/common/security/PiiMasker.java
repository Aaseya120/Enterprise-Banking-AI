package com.bank.common.security;

import java.util.regex.Pattern;

/**
 * Minimal PII masking helper (architecture plan section 28). Real deployments
 * should replace this with a proper DLP/tokenization service; this exists so
 * every AI-facing service has one obvious place to route text through before
 * it reaches a prompt.
 */
public final class PiiMasker {

    private static final Pattern CARD_NUMBER = Pattern.compile("\\b(\\d{4})\\d{8}(\\d{4})\\b");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("\\b\\d{10,16}\\b");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null) {
            return null;
        }
        String masked = CARD_NUMBER.matcher(text).replaceAll("$1********$2");
        masked = EMAIL.matcher(masked).replaceAll("***@***");
        masked = ACCOUNT_NUMBER.matcher(masked).replaceAll(m -> maskAccount(m.group()));
        return masked;
    }

    private static String maskAccount(String digits) {
        if (digits.length() <= 4) {
            return digits;
        }
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}
