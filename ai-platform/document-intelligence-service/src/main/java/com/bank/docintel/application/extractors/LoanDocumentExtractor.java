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
public class LoanDocumentExtractor implements FieldExtractor {

    private static final Pattern APPLICANT = Pattern.compile("(?i)applicant(?: name)?:\\s*([A-Za-z ,.'-]+)");
    private static final Pattern PRINCIPAL = Pattern.compile("(?i)principal(?: amount)?:\\s*\\$?([0-9,.]+)");
    private static final Pattern RATE = Pattern.compile("(?i)interest rate:\\s*([0-9.]+)%?");
    private static final Pattern TENURE = Pattern.compile("(?i)tenure:\\s*([0-9]+)\\s*(?:months|month)");

    @Override
    public BankDocumentType supports() {
        return BankDocumentType.LOAN_DOCUMENT;
    }

    @Override
    public ExtractionResult extract(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        int found = 0;
        found += match(APPLICANT, text, "applicantName", fields);
        found += match(PRINCIPAL, text, "principalAmount", fields);
        found += match(RATE, text, "interestRate", fields);
        found += match(TENURE, text, "tenureMonths", fields);
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
