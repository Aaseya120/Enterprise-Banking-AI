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
public class ChequeExtractor implements FieldExtractor {

    private static final Pattern PAYEE = Pattern.compile("(?i)pay to the order of:?\\s*([A-Za-z ,.'-]+)");
    private static final Pattern AMOUNT = Pattern.compile("(?i)amount:\\s*\\$?([0-9,.]+)");
    private static final Pattern CHEQUE_NO = Pattern.compile("(?i)check no\\.?:\\s*([0-9]+)");
    private static final Pattern DATE = Pattern.compile("(?i)date:\\s*([0-9/.-]+)");

    @Override
    public BankDocumentType supports() {
        return BankDocumentType.CHEQUE;
    }

    @Override
    public ExtractionResult extract(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        int found = 0;
        found += match(PAYEE, text, "payee", fields);
        found += match(AMOUNT, text, "amount", fields);
        found += match(CHEQUE_NO, text, "chequeNumber", fields);
        found += match(DATE, text, "date", fields);
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
