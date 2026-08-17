package com.bank.docintel.application;

import com.bank.docintel.domain.BankDocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Real (if simple) keyword-scoring classifier: counts type-specific keyword
 * hits in the text and picks the highest-scoring type, or UNKNOWN if
 * nothing scores above zero. This is the "Document classification" step
 * of the pipeline (plan section 22) -- not a stub, but also not an ML
 * model; a production system would likely swap this for a trained
 * classifier or an LLM call (routed through ai-model-gateway) without
 * changing DocumentIntelligenceService's orchestration.
 */
@Component
public class DocumentClassifier {

    private static final Map<BankDocumentType, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(BankDocumentType.KYC, List.of(
                "passport", "driver's license", "national id", "date of birth", "identity verification"));
        KEYWORDS.put(BankDocumentType.LOAN_DOCUMENT, List.of(
                "loan application", "principal amount", "interest rate", "tenure", "collateral"));
        KEYWORDS.put(BankDocumentType.CHEQUE, List.of(
                "pay to the order of", "cheque", "check no", "routing number", "payee"));
        KEYWORDS.put(BankDocumentType.STATEMENT, List.of(
                "account statement", "opening balance", "closing balance", "statement period"));
        KEYWORDS.put(BankDocumentType.REPORT, List.of(
                "executive summary", "quarterly report", "annual report", "key findings"));
    }

    public BankDocumentType classify(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        BankDocumentType best = BankDocumentType.UNKNOWN;
        int bestScore = 0;

        for (Map.Entry<BankDocumentType, List<String>> entry : KEYWORDS.entrySet()) {
            int score = (int) entry.getValue().stream().filter(lower::contains).count();
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }
        return best;
    }
}
