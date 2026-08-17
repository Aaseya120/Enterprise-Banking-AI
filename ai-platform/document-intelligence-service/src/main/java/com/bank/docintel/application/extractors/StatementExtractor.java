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
public class StatementExtractor implements FieldExtractor {

    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("(?i)account(?: number)?:\\s*([0-9*]+)");
    private static final Pattern PERIOD = Pattern.compile("(?i)statement period:\\s*([A-Za-z0-9 /,.-]+)");
    private static final Pattern OPENING = Pattern.compile("(?i)opening balance:\\s*\\$?([0-9,.]+)");
    private static final Pattern CLOSING = Pattern.compile("(?i)closing balance:\\s*\\$?([0-9,.]+)");

    @Override
    public BankDocumentType supports() {
        return BankDocumentType.STATEMENT;
    }

    @Override
    public ExtractionResult extract(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        int found = 0;
        found += match(ACCOUNT_NUMBER, text, "accountNumber", fields);
        found += match(PERIOD, text, "statementPeriod", fields);
        found += match(OPENING, text, "openingBalance", fields);
        found += match(CLOSING, text, "closingBalance", fields);
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
