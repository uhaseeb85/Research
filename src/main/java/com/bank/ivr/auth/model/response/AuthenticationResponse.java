package com.bank.ivr.auth.model.response;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class AuthenticationResponse {
    
    public enum AuthStatus {
        PENDING_PRIMARY_TOKEN, 
        AUTHENTICATED, 
        FAILED, 
        PENDING_MORE_TOKENS
    }
    
    @JsonProperty("attemptId")
    private final String attemptId;
    
    @JsonProperty("status")
    private final AuthStatus status;
    
    @JsonProperty("message")
    private final String message;
    
    @JsonProperty("primaryTokenToAsk")
    private final AuthTokenDefinition primaryTokenToAsk;
    
    @JsonProperty("secondaryTokensAccepted")
    private final List<AuthTokenDefinition> secondaryTokensAccepted;
    
    @JsonProperty("remainingAttempts")
    private final Map<String, Integer> remainingAttempts;
    

    
    @JsonProperty("authenticatedTokens")
    private final List<String> authenticatedTokens;
    
    @JsonProperty("failedTokens")
    private final List<String> failedTokens;
    
    private AuthenticationResponse(Builder builder) {
        this.attemptId = builder.attemptId;
        this.status = builder.status;
        this.message = builder.message;
        this.primaryTokenToAsk = builder.primaryTokenToAsk;
        this.secondaryTokensAccepted = builder.secondaryTokensAccepted;
        this.remainingAttempts = builder.remainingAttempts;

        this.authenticatedTokens = builder.authenticatedTokens;
        this.failedTokens = builder.failedTokens;
    }
    
    public String getAttemptId() {
        return attemptId;
    }
    
    public AuthStatus getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public AuthTokenDefinition getPrimaryTokenToAsk() {
        return primaryTokenToAsk;
    }
    
    public List<AuthTokenDefinition> getSecondaryTokensAccepted() {
        return secondaryTokensAccepted;
    }
    
    public Map<String, Integer> getRemainingAttempts() {
        return remainingAttempts;
    }
    

    
    public List<String> getAuthenticatedTokens() {
        return authenticatedTokens;
    }
    
    public List<String> getFailedTokens() {
        return failedTokens;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String attemptId;
        private AuthStatus status;
        private String message;
        private AuthTokenDefinition primaryTokenToAsk;
        private List<AuthTokenDefinition> secondaryTokensAccepted;
        private Map<String, Integer> remainingAttempts;

        private List<String> authenticatedTokens;
        private List<String> failedTokens;
        
        public Builder attemptId(String attemptId) {
            this.attemptId = attemptId;
            return this;
        }
        
        public Builder status(AuthStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder primaryTokenToAsk(AuthTokenDefinition primaryTokenToAsk) {
            this.primaryTokenToAsk = primaryTokenToAsk;
            return this;
        }
        
        public Builder secondaryTokensAccepted(List<AuthTokenDefinition> secondaryTokensAccepted) {
            this.secondaryTokensAccepted = secondaryTokensAccepted;
            return this;
        }
        
        public Builder remainingAttempts(Map<String, Integer> remainingAttempts) {
            this.remainingAttempts = remainingAttempts;
            return this;
        }
        

        
        public Builder authenticatedTokens(List<String> authenticatedTokens) {
            this.authenticatedTokens = authenticatedTokens;
            return this;
        }
        
        public Builder failedTokens(List<String> failedTokens) {
            this.failedTokens = failedTokens;
            return this;
        }
        
        public AuthenticationResponse build() {
            return new AuthenticationResponse(this);
        }
    }
} 