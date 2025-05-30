package com.bank.ivr.auth.model.domain;

import java.util.HashMap;
import java.util.Map;

import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Manages the attempt-related state during authentication.
 * Extracted from AuthenticationContext to improve separation of concerns.
 */
public class AttemptState {
    
    @JsonProperty("tokenAttemptsRemaining")
    private Map<String, Integer> tokenAttemptsRemaining;
    
    @JsonProperty("overallAttemptsRemaining")
    private int overallAttemptsRemaining;
    
    @JsonProperty("currentStatus")
    private AuthStatus currentStatus;
    
    @JsonCreator
    public AttemptState(
            @JsonProperty("tokenAttemptsRemaining") Map<String, Integer> tokenAttemptsRemaining,
            @JsonProperty("overallAttemptsRemaining") int overallAttemptsRemaining,
            @JsonProperty("currentStatus") AuthStatus currentStatus) {
        this.tokenAttemptsRemaining = tokenAttemptsRemaining != null ? tokenAttemptsRemaining : new HashMap<>();
        this.overallAttemptsRemaining = overallAttemptsRemaining;
        this.currentStatus = currentStatus;
    }
    
    // Getters
    public Map<String, Integer> getTokenAttemptsRemaining() {
        return tokenAttemptsRemaining;
    }
    
    public int getOverallAttemptsRemaining() {
        return overallAttemptsRemaining;
    }
    
    public AuthStatus getCurrentStatus() {
        return currentStatus;
    }
    
    // Setters for mutable operations
    public void setTokenAttemptsRemaining(Map<String, Integer> tokenAttemptsRemaining) {
        this.tokenAttemptsRemaining = tokenAttemptsRemaining;
    }
    
    public void setOverallAttemptsRemaining(int overallAttemptsRemaining) {
        this.overallAttemptsRemaining = overallAttemptsRemaining;
    }
    
    public void setCurrentStatus(AuthStatus currentStatus) {
        this.currentStatus = currentStatus;
    }
    
    // Business logic methods
    public boolean decrementTokenAttempts(String tokenName) {
        Integer remaining = tokenAttemptsRemaining.get(tokenName);
        if (remaining != null && remaining > 0) {
            tokenAttemptsRemaining.put(tokenName, remaining - 1);
            return true;
        }
        return false;
    }
    
    public boolean decrementOverallAttempts() {
        if (overallAttemptsRemaining > 0) {
            overallAttemptsRemaining--;
            return true;
        }
        return false;
    }
    
    public boolean hasRemainingAttemptsForToken(String tokenName) {
        Integer remaining = tokenAttemptsRemaining.get(tokenName);
        return remaining != null && remaining > 0;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Map<String, Integer> tokenAttemptsRemaining = new HashMap<>();
        private int overallAttemptsRemaining = 5; // default
        private AuthStatus currentStatus = AuthStatus.PENDING_PRIMARY_TOKEN;
        
        public Builder tokenAttemptsRemaining(Map<String, Integer> tokenAttemptsRemaining) {
            this.tokenAttemptsRemaining = tokenAttemptsRemaining;
            return this;
        }
        
        public Builder overallAttemptsRemaining(int overallAttemptsRemaining) {
            this.overallAttemptsRemaining = overallAttemptsRemaining;
            return this;
        }
        
        public Builder currentStatus(AuthStatus currentStatus) {
            this.currentStatus = currentStatus;
            return this;
        }
        
        public AttemptState build() {
            return new AttemptState(tokenAttemptsRemaining, overallAttemptsRemaining, currentStatus);
        }
    }
} 