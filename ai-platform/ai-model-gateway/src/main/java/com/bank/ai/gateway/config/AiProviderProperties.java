package com.bank.ai.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from application.yml (bank.ai.providers.*). Values should be sourced
 * from Vault/Secrets Manager in real deployments -- never committed to git
 * (plan section 36).
 */
@ConfigurationProperties(prefix = "bank.ai.providers")
public class AiProviderProperties {

    private Provider openai = new Provider();
    private Provider claude = new Provider();
    private Provider gemini = new Provider();
    private String defaultProvider = "OPENAI";
    private String fallbackProvider = "CLAUDE";

    public static class Provider {
        private String apiKey;
        private String baseUrl;
        private String model;
        private boolean enabled = false;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public Provider getOpenai() { return openai; }
    public void setOpenai(Provider openai) { this.openai = openai; }
    public Provider getClaude() { return claude; }
    public void setClaude(Provider claude) { this.claude = claude; }
    public Provider getGemini() { return gemini; }
    public void setGemini(Provider gemini) { this.gemini = gemini; }
    public String getDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }
    public String getFallbackProvider() { return fallbackProvider; }
    public void setFallbackProvider(String fallbackProvider) { this.fallbackProvider = fallbackProvider; }
}
