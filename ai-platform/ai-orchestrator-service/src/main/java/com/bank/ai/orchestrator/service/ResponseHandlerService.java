package com.bank.ai.orchestrator.service;

import com.bank.ai.gateway.model.AiResponse;
import com.bank.common.security.PiiMasker;
import org.springframework.stereotype.Service;

/**
 * Implements the "Response Handler" box from section 5/29: parses, masks any
 * PII that slipped into a generated answer, and flags whether the answer
 * was actually grounded in retrieved context (vs. a raw model guess) so the
 * orchestrator can decide whether to show a "not verified" disclaimer.
 */
@Service
public class ResponseHandlerService {

    public String finalize(AiResponse response, boolean hadContext) {
        String masked = PiiMasker.mask(response.content());
        if (hadContext && !response.grounded()) {
            return masked + "\n\n(Note: this answer could not be fully grounded in retrieved policy "
                    + "documents -- please verify with a bank representative for anything time-sensitive.)";
        }
        return masked;
    }
}
