package com.bank.ai.fraud.model;

public record RiskScore(String transferId, double score, RiskLevel level, String reason) {

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    public boolean isApproved() {
        return level != RiskLevel.HIGH;
    }
}
