package com.bank.ivr.auth.model.domain;

import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthenticationContext {
    
    @JsonProperty("attemptId")
    private final String attemptId;
    
    @JsonProperty("sessionId")
    private final String sessionId;
    
    @JsonProperty("customerIdentifier")
    private final CustomerIdentifier customerIdentifier;
    
    @JsonProperty("brand")
    private final String brand;
    
    @JsonProperty("startTime")
    private final LocalDateTime startTime;
    
    @JsonProperty("tokenAttemptsRemaining")
    private Map<String, Integer> tokenAttemptsRemaining;
    
    @JsonProperty("overallAttemptsRemaining")
    private int overallAttemptsRemaining;
    
    @JsonProperty("eligibleTokens")
    private List<String> eligibleTokens;
    
    @JsonProperty("authenticatedTokens")
    private List<String> authenticatedTokens;
    
    @JsonProperty("requiredTokensForFullAuth")
    private List<String> requiredTokensForFullAuth;
    
    @JsonProperty("lastAskedToken")
    private String lastAskedToken;
    
    @JsonProperty("currentStatus")
    private AuthStatus currentStatus;
    
    @JsonProperty("failedTokens")
    private List<String> failedTokens;
    
    @JsonProperty("askedTokens")
    private List<String> askedTokens;
    
    @JsonProperty("tokenRetryStates")
    private Map<String, TokenRetryState> tokenRetryStates;
    
    @JsonProperty("globalRetryState")
    private GlobalRetryState globalRetryState;
    
    @JsonProperty("askedTokensWithValidationFailure")
    private Map<String, Integer> askedTokensWithValidationFailure;
    
    @JsonCreator
    public AuthenticationContext(
            @JsonProperty("attemptId") String attemptId,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("customerIdentifier") CustomerIdentifier customerIdentifier,
            @JsonProperty("brand") String brand,
            @JsonProperty("startTime") LocalDateTime startTime,
            @JsonProperty("tokenAttemptsRemaining") Map<String, Integer> tokenAttemptsRemaining,
            @JsonProperty("overallAttemptsRemaining") int overallAttemptsRemaining,
            @JsonProperty("eligibleTokens") List<String> eligibleTokens,
            @JsonProperty("authenticatedTokens") List<String> authenticatedTokens,
            @JsonProperty("requiredTokensForFullAuth") List<String> requiredTokensForFullAuth,
            @JsonProperty("lastAskedToken") String lastAskedToken,
            @JsonProperty("currentStatus") AuthStatus currentStatus,
            @JsonProperty("failedTokens") List<String> failedTokens,
            @JsonProperty("askedTokens") List<String> askedTokens,
            @JsonProperty("askedTokensWithValidationFailure") Map<String, Integer> askedTokensWithValidationFailure) {
        this.attemptId = attemptId;
        this.sessionId = sessionId;
        this.customerIdentifier = customerIdentifier;
        this.brand = brand;
        this.startTime = startTime;
        this.tokenAttemptsRemaining = tokenAttemptsRemaining != null ? tokenAttemptsRemaining : new HashMap<>();
        this.overallAttemptsRemaining = overallAttemptsRemaining;
        this.eligibleTokens = eligibleTokens != null ? eligibleTokens : new ArrayList<>();
        this.authenticatedTokens = authenticatedTokens != null ? authenticatedTokens : new ArrayList<>();
        this.requiredTokensForFullAuth = requiredTokensForFullAuth != null ? requiredTokensForFullAuth : new ArrayList<>();
        this.lastAskedToken = lastAskedToken;
        this.currentStatus = currentStatus;
        this.failedTokens = failedTokens != null ? failedTokens : new ArrayList<>();
        this.askedTokens = askedTokens != null ? askedTokens : new ArrayList<>();
        this.askedTokensWithValidationFailure = askedTokensWithValidationFailure != null ? askedTokensWithValidationFailure : new HashMap<>();
    }
    
    // Builder constructor
    private AuthenticationContext(Builder builder) {
        this.attemptId = builder.attemptId;
        this.sessionId = builder.sessionId;
        this.customerIdentifier = builder.customerIdentifier;
        this.brand = builder.brand;
        this.startTime = builder.startTime;
        this.tokenAttemptsRemaining = builder.tokenAttemptsRemaining;
        this.overallAttemptsRemaining = builder.overallAttemptsRemaining;
        this.eligibleTokens = builder.eligibleTokens;
        this.authenticatedTokens = builder.authenticatedTokens;
        this.requiredTokensForFullAuth = builder.requiredTokensForFullAuth;
        this.lastAskedToken = builder.lastAskedToken;
        this.currentStatus = builder.currentStatus;
        this.failedTokens = builder.failedTokens;
        this.askedTokens = builder.askedTokens;
        this.askedTokensWithValidationFailure = builder.askedTokensWithValidationFailure;
    }
    
    // Getters
    public String getAttemptId() {
        return attemptId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public CustomerIdentifier getCustomerIdentifier() {
        return customerIdentifier;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public Map<String, Integer> getTokenAttemptsRemaining() {
        return tokenAttemptsRemaining;
    }
    
    public int getOverallAttemptsRemaining() {
        return overallAttemptsRemaining;
    }
    
    public List<String> getEligibleTokens() {
        return eligibleTokens;
    }
    
    public List<String> getAuthenticatedTokens() {
        return authenticatedTokens;
    }
    
    public List<String> getRequiredTokensForFullAuth() {
        return requiredTokensForFullAuth;
    }
    
    public String getLastAskedToken() {
        return lastAskedToken;
    }
    
    public AuthStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public List<String> getFailedTokens() {
        return failedTokens;
    }
    
    public List<String> getAskedTokens() {
        return askedTokens;
    }
    
    public Map<String, TokenRetryState> getTokenRetryStates() {
        return tokenRetryStates;
    }
    
    public GlobalRetryState getGlobalRetryState() {
        return globalRetryState;
    }
    
    public Map<String, Integer> getAskedTokensWithValidationFailure() {
        return askedTokensWithValidationFailure;
    }
    
    // Setters for mutable operations
    public void setTokenAttemptsRemaining(Map<String, Integer> tokenAttemptsRemaining) {
        this.tokenAttemptsRemaining = tokenAttemptsRemaining;
    }
    
    public void setOverallAttemptsRemaining(int overallAttemptsRemaining) {
        this.overallAttemptsRemaining = overallAttemptsRemaining;
    }
    
    public void setEligibleTokens(List<String> eligibleTokens) {
        this.eligibleTokens = eligibleTokens;
    }
    
    public void setAuthenticatedTokens(List<String> authenticatedTokens) {
        this.authenticatedTokens = authenticatedTokens;
    }
    
    public void setRequiredTokensForFullAuth(List<String> requiredTokensForFullAuth) {
        this.requiredTokensForFullAuth = requiredTokensForFullAuth;
    }
    
    public void setLastAskedToken(String lastAskedToken) {
        this.lastAskedToken = lastAskedToken;
    }
    
    public void setCurrentStatus(AuthStatus currentStatus) {
        this.currentStatus = currentStatus;
    }
    
    public void setFailedTokens(List<String> failedTokens) {
        this.failedTokens = failedTokens;
    }
    
    public void setAskedTokens(List<String> askedTokens) {
        this.askedTokens = askedTokens;
    }
    
    public void setTokenRetryStates(Map<String, TokenRetryState> tokenRetryStates) {
        this.tokenRetryStates = tokenRetryStates;
    }
    
    public void setGlobalRetryState(GlobalRetryState globalRetryState) {
        this.globalRetryState = globalRetryState;
    }
    
    public void setAskedTokensWithValidationFailure(Map<String, Integer> askedTokensWithValidationFailure) {
        this.askedTokensWithValidationFailure = askedTokensWithValidationFailure;
    }
    
    // Helper methods
    public boolean decrementTokenAttempts(String tokenName) {
        Integer remaining = tokenAttemptsRemaining.get(tokenName);
        if (remaining != null && remaining > 0) {
            tokenAttemptsRemaining.put(tokenName, remaining - 1);
            return tokenAttemptsRemaining.get(tokenName) >= 0;
        }
        return false;
    }
    
    public boolean decrementOverallAttempts() {
        if (overallAttemptsRemaining > 0) {
            overallAttemptsRemaining--;
            return overallAttemptsRemaining >= 0;
        }
        return false;
    }
    
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
    
    public boolean hasRemainingAttemptsForToken(String tokenName) {
        Integer remaining = tokenAttemptsRemaining.get(tokenName);
        return remaining != null && remaining > 0;
    }
    
    public void addAskedToken(String tokenName) {
        if (!askedTokens.contains(tokenName)) {
            askedTokens.add(tokenName);
        }
    }
    
    public boolean isTokenAlreadyAsked(String tokenName) {
        return askedTokens.contains(tokenName);
    }
    
    /**
     * Records that a token failed validation after being specifically requested.
     * This is used to prevent re-asking tokens that the user provided but failed validation.
     */
    public void markAskedTokenValidationFailure(String tokenName) {
        askedTokensWithValidationFailure.put(tokenName, 
            askedTokensWithValidationFailure.getOrDefault(tokenName, 0) + 1);
    }
    
    /**
     * Checks if a token has failed validation after being specifically asked.
     * Returns true if the user provided this token in response to our request but it failed validation.
     */
    public boolean hasAskedTokenValidationFailure(String tokenName) {
        return askedTokensWithValidationFailure.containsKey(tokenName) && 
               askedTokensWithValidationFailure.get(tokenName) > 0;
    }
    
    /**
     * Gets the number of validation failures for a specific asked token.
     */
    public int getAskedTokenValidationFailureCount(String tokenName) {
        return askedTokensWithValidationFailure.getOrDefault(tokenName, 0);
    }
    
    /**
     * Determines if a token can be re-asked based on the smart re-asking logic:
     * - Can re-ask if user didn't provide the token we specifically asked for
     * - Cannot re-ask if user provided the requested token but it failed validation
     */
    public boolean canReAskToken(String tokenName) {
        // If we haven't asked for this token before, we can ask
        if (!isTokenAlreadyAsked(tokenName)) {
            return true;
        }
        
        // If we asked for this token but user didn't provide it (no validation failure recorded),
        // we can ask again
        if (!hasAskedTokenValidationFailure(tokenName)) {
            return true;
        }
        
        // If user provided the token we asked for but it failed validation, don't ask again
        return false;
    }
    
    /**
     * Resets the asked tokens list for a new attempt.
     * This allows asking for the same tokens in a new attempt if appropriate.
     */
    public void resetAskedTokensForNewAttempt() {
        askedTokens.clear();
        // Note: We keep askedTokensWithValidationFailure to track validation failures across attempts
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String attemptId;
        private String sessionId;
        private CustomerIdentifier customerIdentifier;
        private String brand;
        private LocalDateTime startTime;
        private Map<String, Integer> tokenAttemptsRemaining = new HashMap<>();
        private int overallAttemptsRemaining = 5; // default
        private List<String> eligibleTokens = new ArrayList<>();
        private List<String> authenticatedTokens = new ArrayList<>();
        private List<String> requiredTokensForFullAuth = new ArrayList<>();
        private String lastAskedToken;
        private AuthStatus currentStatus = AuthStatus.PENDING_PRIMARY_TOKEN;
        private List<String> failedTokens = new ArrayList<>();
        private List<String> askedTokens = new ArrayList<>();
        private Map<String, Integer> askedTokensWithValidationFailure = new HashMap<>();
        
        public Builder attemptId(String attemptId) {
            this.attemptId = attemptId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder customerIdentifier(CustomerIdentifier customerIdentifier) {
            this.customerIdentifier = customerIdentifier;
            return this;
        }
        
        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder tokenAttemptsRemaining(Map<String, Integer> tokenAttemptsRemaining) {
            this.tokenAttemptsRemaining = tokenAttemptsRemaining;
            return this;
        }
        
        public Builder overallAttemptsRemaining(int overallAttemptsRemaining) {
            this.overallAttemptsRemaining = overallAttemptsRemaining;
            return this;
        }
        
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
        
        public Builder lastAskedToken(String lastAskedToken) {
            this.lastAskedToken = lastAskedToken;
            return this;
        }
        
        public Builder currentStatus(AuthStatus currentStatus) {
            this.currentStatus = currentStatus;
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
        
        public Builder askedTokensWithValidationFailure(Map<String, Integer> askedTokensWithValidationFailure) {
            this.askedTokensWithValidationFailure = askedTokensWithValidationFailure;
            return this;
        }
        
        public AuthenticationContext build() {
            if (attemptId == null || sessionId == null || customerIdentifier == null) {
                throw new IllegalArgumentException("attemptId, sessionId, and customerIdentifier are required");
            }
            if (startTime == null) {
                this.startTime = LocalDateTime.now();
            }
            return new AuthenticationContext(this);
        }
    }
    
    @Override
    public String toString() {
        return "AuthenticationContext{" +
               "attemptId='" + attemptId + '\'' +
               ", sessionId='" + sessionId + '\'' +
               ", customerIdentifier=" + customerIdentifier +
               ", brand='" + brand + '\'' +
               ", startTime=" + startTime +
               ", tokenAttemptsRemaining=" + tokenAttemptsRemaining +
               ", overallAttemptsRemaining=" + overallAttemptsRemaining +
               ", eligibleTokens=" + eligibleTokens +
               ", authenticatedTokens=" + authenticatedTokens +
               ", requiredTokensForFullAuth=" + requiredTokensForFullAuth +
               ", lastAskedToken='" + lastAskedToken + '\'' +
               ", currentStatus=" + currentStatus +
               ", failedTokens=" + failedTokens +
               ", askedTokens=" + askedTokens +
               '}';
    }
} 