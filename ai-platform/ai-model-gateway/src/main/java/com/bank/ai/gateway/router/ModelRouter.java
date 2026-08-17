package com.bank.ai.gateway.router;

import com.bank.ai.gateway.client.AiModelClient;
import com.bank.ai.gateway.model.AiRequest;

public interface ModelRouter {

    /** Select the client that should handle this request. */
    AiModelClient selectModel(AiRequest request);

    /** Client to use if the selected model's provider call fails. */
    AiModelClient fallbackModel(AiRequest request);
}
