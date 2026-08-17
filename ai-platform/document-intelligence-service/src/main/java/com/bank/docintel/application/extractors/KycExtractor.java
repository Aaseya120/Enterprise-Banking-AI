package com.bank.docintel.application.extractors;

import com.bank.docintel.application.ExtractionResult;
import com.bank.docintel.application.FieldExtractor;
import com.bank.docintel.domain.BankDocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KycExtractor implements FieldExtractor {

    private static final Pattern NAME = Pattern.compile("(?i)name:\\s*([A-Za-z ,.'-]+)");
    private static final Pattern DOC_NUMBER = Pattern.compile("(?i)(?:document|passport|license|id)\\s*(?:no\\.?|number):\\s*([A-Za-z0-9-]+)");
    private static final Pattern DOB = Pattern.compile("(?i)date of birth:\\s*([0-9/.-]+)");
    private static final Pattern EXPIRY = Pattern.compile("(?i)expiry(?: date)?:\\s*([0-9/.-]+)");

    @Override
    public BankDocumentType supports() {
        return BankDocumentType.KYC;
    }

    @Override
    public ExtractionResult extract(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        int found = 0;
        found += match(NAME, text, "customerName", fields);
        found += match(DOC_NUMBER, text, "documentNumber", fields);
        found += match(DOB, text, "dateOfBirth", fields);
        found += match(EXPIRY, text, "expiryDate", fields);
        return new ExtractionResult(fields, found / 4.0);
    }

    private int match(Pattern pattern, String text, String key, Map<String, String> fields) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            fields.put(key, matcher.group(1).trim());
            return 1;
        }
        return 0;
    }
}
