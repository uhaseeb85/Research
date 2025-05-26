package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the state of tokens during authentication.
 * Extracted from AuthenticationContext to improve separation of concerns.
 */
public class TokenState {
    
    @JsonProperty("eligibleTokens")
    private List<String> eligibleTokens;
    
    @JsonProperty("authenticatedTokens")
    private List<String> authenticatedTokens;
    
    @JsonProperty("requiredTokensForFullAuth")
    private List<String> requiredTokensForFullAuth;
    
    @JsonProperty("failedTokens")
    private List<String> failedTokens;
    
    @JsonProperty("askedTokens")
    private List<String> askedTokens;
    
    @JsonProperty("lastAskedToken")
    private String lastAskedToken;
    
    @JsonProperty("askedTokensWithValidationFailure")
    private Map<String, Integer> askedTokensWithValidationFailure;
    
    @JsonCreator
    public TokenState(
            @JsonProperty("eligibleTokens") List<String> eligibleTokens,
            @JsonProperty("authenticatedTokens") List<String> authenticatedTokens,
            @JsonProperty("requiredTokensForFullAuth") List<String> requiredTokensForFullAuth,
            @JsonProperty("failedTokens") List<String> failedTokens,
            @JsonProperty("askedTokens") List<String> askedTokens,
            @JsonProperty("lastAskedToken") String lastAskedToken,
            @JsonProperty("askedTokensWithValidationFailure") Map<String, Integer> askedTokensWithValidationFailure) {
        this.eligibleTokens = eligibleTokens != null ? eligibleTokens : new ArrayList<>();
        this.authenticatedTokens = authenticatedTokens != null ? authenticatedTokens : new ArrayList<>();
        this.requiredTokensForFullAuth = requiredTokensForFullAuth != null ? requiredTokensForFullAuth : new ArrayList<>();
        this.failedTokens = failedTokens != null ? failedTokens : new ArrayList<>();
        this.askedTokens = askedTokens != null ? askedTokens : new ArrayList<>();
        this.lastAskedToken = lastAskedToken;
        this.askedTokensWithValidationFailure = askedTokensWithValidationFailure != null ? askedTokensWithValidationFailure : new HashMap<>();
    }
    
    // Getters
    public List<String> getEligibleTokens() {
        return eligibleTokens;
    }
    
    public List<String> getAuthenticatedTokens() {
        return authenticatedTokens;
    }
    
    public List<String> getRequiredTokensForFullAuth() {
        return requiredTokensForFullAuth;
    }
    
    public List<String> getFailedTokens() {
        return failedTokens;
    }
    
    public List<String> getAskedTokens() {
        return askedTokens;
    }
    
    public String getLastAskedToken() {
        return lastAskedToken;
    }
    
    public Map<String, Integer> getAskedTokensWithValidationFailure() {
        return askedTokensWithValidationFailure;
    }
    
    // Setters for mutable operations
    public void setEligibleTokens(List<String> eligibleTokens) {
        this.eligibleTokens = eligibleTokens;
    }
    
    public void setAuthenticatedTokens(List<String> authenticatedTokens) {
        this.authenticatedTokens = authenticatedTokens;
    }
    
    public void setRequiredTokensForFullAuth(List<String> requiredTokensForFullAuth) {
        this.requiredTokensForFullAuth = requiredTokensForFullAuth;
    }
    
    public void setFailedTokens(List<String> failedTokens) {
        this.failedTokens = failedTokens;
    }
    
    public void setAskedTokens(List<String> askedTokens) {
        this.askedTokens = askedTokens;
    }
    
    public void setLastAskedToken(String lastAskedToken) {
        this.lastAskedToken = lastAskedToken;
    }
    
    public void setAskedTokensWithValidationFailure(Map<String, Integer> askedTokensWithValidationFailure) {
        this.askedTokensWithValidationFailure = askedTokensWithValidationFailure;
    }
    
    // Business logic methods
    public void addAuthenticatedToken(String tokenName) {
        if (!authenticatedTokens.contains(tokenName)) {
            authenticatedTokens.add(tokenName);
        }
    }
    
    public void addFailedToken(String tokenName) {
        if (!failedTokens.contains(tokenName)) {
            failedTokens.add(tokenName);
        }
    }
    
    public boolean isTokenAuthenticated(String tokenName) {
        return authenticatedTokens.contains(tokenName);
    }
    
    public boolean isTokenFailed(String tokenName) {
        return failedTokens.contains(tokenName);
    }
    
    public void addAskedToken(String tokenName) {
        if (!askedTokens.contains(tokenName)) {
            askedTokens.add(tokenName);
        }
    }
    
    public boolean isTokenAlreadyAsked(String tokenName) {
        return askedTokens.contains(tokenName);
    }
    
    public void markAskedTokenValidationFailure(String tokenName) {
        askedTokensWithValidationFailure.put(tokenName, 
                askedTokensWithValidationFailure.getOrDefault(tokenName, 0) + 1);
    }
    
    public boolean hasAskedTokenValidationFailure(String tokenName) {
        return askedTokensWithValidationFailure.containsKey(tokenName) && 
               askedTokensWithValidationFailure.get(tokenName) > 0;
    }
    
    public int getAskedTokenValidationFailureCount(String tokenName) {
        return askedTokensWithValidationFailure.getOrDefault(tokenName, 0);
    }
    
    public boolean canReAskToken(String tokenName) {
        // Smart re-asking logic: don't re-ask tokens that have validation failures
        // If a user provided a token and it failed validation, never re-ask it
        if (hasAskedTokenValidationFailure(tokenName)) {
            return false;
        }
        
        // If no validation failure, can re-ask
        return true;
    }
    
    public void resetAskedTokensForNewAttempt() {
        askedTokens.clear();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<String> eligibleTokens = new ArrayList<>();
        private List<String> authenticatedTokens = new ArrayList<>();
        private List<String> requiredTokensForFullAuth = new ArrayList<>();
        private List<String> failedTokens = new ArrayList<>();
        private List<String> askedTokens = new ArrayList<>();
        private String lastAskedToken;
        private Map<String, Integer> askedTokensWithValidationFailure = new HashMap<>();
        
        public Builder eligibleTokens(List<String> eligibleTokens) {
            this.eligibleTokens = eligibleTokens;
            return this;
        }
        
        public Builder authenticatedTokens(List<String> authenticatedTokens) {
            this.authenticatedTokens = authenticatedTokens;
            return this;
        }
        
        public Builder requiredTokensForFullAuth(List<String> requiredTokensForFullAuth) {
            this.requiredTokensForFullAuth = requiredTokensForFullAuth;
            return this;
        }
        
        public Builder failedTokens(List<String> failedTokens) {
            this.failedTokens = failedTokens;
            return this;
        }
        
        public Builder askedTokens(List<String> askedTokens) {
            this.askedTokens = askedTokens;
            return this;
        }
        
        public Builder lastAskedToken(String lastAskedToken) {
            this.lastAskedToken = lastAskedToken;
            return this;
        }
        
        public Builder askedTokensWithValidationFailure(Map<String, Integer> askedTokensWithValidationFailure) {
            this.askedTokensWithValidationFailure = askedTokensWithValidationFailure;
            return this;
        }
        
        public TokenState build() {
            return new TokenState(eligibleTokens, authenticatedTokens, requiredTokensForFullAuth,
                    failedTokens, askedTokens, lastAskedToken, askedTokensWithValidationFailure);
        }
    }
} 