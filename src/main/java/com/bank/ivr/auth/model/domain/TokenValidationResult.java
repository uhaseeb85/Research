package com.bank.ivr.auth.model.domain;

import java.util.List;

/**
 * Enhanced result of token validation that includes information about
 * whether additional authentication tokens should be requested.
 * This supports scenarios where successful validation of one token
 * may trigger the need for additional tokens based on trust levels,
 * phone matching, and customer profile attributes.
 */
public class TokenValidationResult {
    
    private final boolean isValid;
    private final boolean requiresAdditionalTokens;
    private final List<String> suggestedAdditionalTokens;
    private final String reason;
    private final String riskLevel;
    
    private TokenValidationResult(Builder builder) {
        this.isValid = builder.isValid;
        this.requiresAdditionalTokens = builder.requiresAdditionalTokens;
        this.suggestedAdditionalTokens = builder.suggestedAdditionalTokens;
        this.reason = builder.reason;
        this.riskLevel = builder.riskLevel;
    }
    
    /**
     * Creates a simple successful validation result.
     */
    public static TokenValidationResult success() {
        return new Builder()
                .isValid(true)
                .requiresAdditionalTokens(false)
                .build();
    }
    
    /**
     * Creates a simple failed validation result.
     */
    public static TokenValidationResult failure() {
        return new Builder()
                .isValid(false)
                .requiresAdditionalTokens(false)
                .build();
    }
    
    /**
     * Creates a successful validation result that requires additional tokens.
     */
    public static TokenValidationResult successWithAdditionalTokensRequired(List<String> suggestedTokens, String reason) {
        return new Builder()
                .isValid(true)
                .requiresAdditionalTokens(true)
                .suggestedAdditionalTokens(suggestedTokens)
                .reason(reason)
                .build();
    }
    
    /**
     * Creates a successful validation result that requires additional tokens with risk level.
     */
    public static TokenValidationResult successWithAdditionalTokensRequired(List<String> suggestedTokens, String reason, String riskLevel) {
        return new Builder()
                .isValid(true)
                .requiresAdditionalTokens(true)
                .suggestedAdditionalTokens(suggestedTokens)
                .reason(reason)
                .riskLevel(riskLevel)
                .build();
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public boolean requiresAdditionalTokens() {
        return requiresAdditionalTokens;
    }
    
    public List<String> getSuggestedAdditionalTokens() {
        return suggestedAdditionalTokens;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean isValid;
        private boolean requiresAdditionalTokens;
        private List<String> suggestedAdditionalTokens;
        private String reason;
        private String riskLevel;
        
        public Builder isValid(boolean isValid) {
            this.isValid = isValid;
            return this;
        }
        
        public Builder requiresAdditionalTokens(boolean requiresAdditionalTokens) {
            this.requiresAdditionalTokens = requiresAdditionalTokens;
            return this;
        }
        
        public Builder suggestedAdditionalTokens(List<String> suggestedAdditionalTokens) {
            this.suggestedAdditionalTokens = suggestedAdditionalTokens;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }
        
        public TokenValidationResult build() {
            return new TokenValidationResult(this);
        }
    }
    
    @Override
    public String toString() {
        return "TokenValidationResult{" +
               "isValid=" + isValid +
               ", requiresAdditionalTokens=" + requiresAdditionalTokens +
               ", suggestedAdditionalTokens=" + suggestedAdditionalTokens +
               ", reason='" + reason + '\'' +
               ", riskLevel='" + riskLevel + '\'' +
               '}';
    }
} 