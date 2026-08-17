package com.bank.ai.orchestrator.service;

import com.bank.ai.orchestrator.model.Intent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Rule-based intent classifier for this scaffold. A production system would
 * typically use a small fine-tuned classifier or a cheap LLM call here
 * (routed via ModelRouter's SIMPLE_FAQ policy) -- the important part is that
 * AiOrchestratorService only depends on this interface-shaped behavior:
 * classify(query) -> Intent.
 */
@Service
public class IntentDetectionService {

    private static final List<String> ACCOUNT_KEYWORDS =
            List.of("balance", "my account", "my accounts", "account number");

    public Intent classify(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        if (ACCOUNT_KEYWORDS.stream().anyMatch(lower::contains)) {
            return Intent.ACCOUNT_LOOKUP;
        }
        if (lower.contains("policy") || lower.contains("eligibility") || lower.contains("sop")
                || lower.contains("faq") || lower.contains("require")) {
            return Intent.POLICY_OR_FAQ;
        }
        return Intent.GENERAL;
    }
}
