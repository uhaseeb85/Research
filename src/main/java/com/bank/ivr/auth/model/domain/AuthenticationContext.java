package com.bank.ivr.auth.model.domain;

import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Simplified AuthenticationContext that delegates to specialized state classes.
 * This decomposition improves maintainability and separation of concerns.
 */
public class AuthenticationContext {
    
    @JsonProperty("session")
    private final AuthenticationSession session;
    
    @JsonProperty("tokenState")
    private TokenState tokenState;
    
    @JsonProperty("attemptState")
    private AttemptState attemptState;
    
    @JsonCreator
    public AuthenticationContext(
            @JsonProperty("session") AuthenticationSession session,
            @JsonProperty("tokenState") TokenState tokenState,
            @JsonProperty("attemptState") AttemptState attemptState) {
        this.session = session;
        this.tokenState = tokenState;
        this.attemptState = attemptState;
    }
    
    // Delegate getters to session
    public String getAttemptId() {
        return session.getAttemptId();
    }
    
    public String getSessionId() {
        return session.getSessionId();
    }
    
    public CustomerIdentifier getCustomerIdentifier() {
        return session.getCustomerIdentifier();
    }
    
    public String getBrand() {
        return session.getBrand();
    }
    
    public LocalDateTime getStartTime() {
        return session.getStartTime();
    }
    
    // Delegate getters to tokenState
    public List<String> getEligibleTokens() {
        return tokenState.getEligibleTokens();
    }
    
    public List<String> getAuthenticatedTokens() {
        return tokenState.getAuthenticatedTokens();
    }
    
    public List<String> getRequiredTokensForFullAuth() {
        return tokenState.getRequiredTokensForFullAuth();
    }
    
    public List<String> getFailedTokens() {
        return tokenState.getFailedTokens();
    }
    
    public List<String> getAskedTokens() {
        return tokenState.getAskedTokens();
    }
    
    public String getLastAskedToken() {
        return tokenState.getLastAskedToken();
    }
    
    public Map<String, Integer> getAskedTokensWithValidationFailure() {
        return tokenState.getAskedTokensWithValidationFailure();
    }
    
    // Delegate getters to attemptState
    public Map<String, Integer> getTokenAttemptsRemaining() {
        return attemptState.getTokenAttemptsRemaining();
    }
    
    public int getOverallAttemptsRemaining() {
        return attemptState.getOverallAttemptsRemaining();
    }
    
    public AuthStatus getCurrentStatus() {
        return attemptState.getCurrentStatus();
    }
    
    public Map<String, TokenRetryState> getTokenRetryStates() {
        return attemptState.getTokenRetryStates();
    }
    
    public GlobalRetryState getGlobalRetryState() {
        return attemptState.getGlobalRetryState();
    }
    
    // Delegate setters to tokenState
    public void setEligibleTokens(List<String> eligibleTokens) {
        tokenState.setEligibleTokens(eligibleTokens);
    }
    
    public void setAuthenticatedTokens(List<String> authenticatedTokens) {
        tokenState.setAuthenticatedTokens(authenticatedTokens);
    }
    
    public void setRequiredTokensForFullAuth(List<String> requiredTokensForFullAuth) {
        tokenState.setRequiredTokensForFullAuth(requiredTokensForFullAuth);
    }
    
    public void setFailedTokens(List<String> failedTokens) {
        tokenState.setFailedTokens(failedTokens);
    }
    
    public void setAskedTokens(List<String> askedTokens) {
        tokenState.setAskedTokens(askedTokens);
    }
    
    public void setLastAskedToken(String lastAskedToken) {
        tokenState.setLastAskedToken(lastAskedToken);
    }
    
    public void setAskedTokensWithValidationFailure(Map<String, Integer> askedTokensWithValidationFailure) {
        tokenState.setAskedTokensWithValidationFailure(askedTokensWithValidationFailure);
    }
    
    // Delegate setters to attemptState
    public void setTokenAttemptsRemaining(Map<String, Integer> tokenAttemptsRemaining) {
        attemptState.setTokenAttemptsRemaining(tokenAttemptsRemaining);
    }
    
    public void setOverallAttemptsRemaining(int overallAttemptsRemaining) {
        attemptState.setOverallAttemptsRemaining(overallAttemptsRemaining);
    }
    
    public void setCurrentStatus(AuthStatus currentStatus) {
        attemptState.setCurrentStatus(currentStatus);
    }
    
    public void setTokenRetryStates(Map<String, TokenRetryState> tokenRetryStates) {
        attemptState.setTokenRetryStates(tokenRetryStates);
    }
    
    public void setGlobalRetryState(GlobalRetryState globalRetryState) {
        attemptState.setGlobalRetryState(globalRetryState);
    }
    
    // Delegate business logic methods to tokenState
    public void addAuthenticatedToken(String tokenName) {
        tokenState.addAuthenticatedToken(tokenName);
    }
    
    public void addFailedToken(String tokenName) {
        tokenState.addFailedToken(tokenName);
    }
    
    public boolean isTokenAuthenticated(String tokenName) {
        return tokenState.isTokenAuthenticated(tokenName);
    }
    
    public boolean isTokenFailed(String tokenName) {
        return tokenState.isTokenFailed(tokenName);
    }
    
    public void addAskedToken(String tokenName) {
        tokenState.addAskedToken(tokenName);
    }
    
    public boolean isTokenAlreadyAsked(String tokenName) {
        return tokenState.isTokenAlreadyAsked(tokenName);
    }
    
    public void markAskedTokenValidationFailure(String tokenName) {
        tokenState.markAskedTokenValidationFailure(tokenName);
    }
    
    public boolean hasAskedTokenValidationFailure(String tokenName) {
        return tokenState.hasAskedTokenValidationFailure(tokenName);
    }
    
    public int getAskedTokenValidationFailureCount(String tokenName) {
        return tokenState.getAskedTokenValidationFailureCount(tokenName);
    }
    
    public boolean canReAskToken(String tokenName) {
        return tokenState.canReAskToken(tokenName);
    }
    
    public void resetAskedTokensForNewAttempt() {
        tokenState.resetAskedTokensForNewAttempt();
    }
    
    // Delegate business logic methods to attemptState
    public boolean decrementTokenAttempts(String tokenName) {
        return attemptState.decrementTokenAttempts(tokenName);
    }
    
    public boolean decrementOverallAttempts() {
        return attemptState.decrementOverallAttempts();
    }
    
    public boolean hasRemainingAttemptsForToken(String tokenName) {
        return attemptState.hasRemainingAttemptsForToken(tokenName);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private AuthenticationSession session;
        private TokenState tokenState;
        private AttemptState attemptState;
        
        public Builder session(AuthenticationSession session) {
            this.session = session;
            return this;
        }
        
        public Builder tokenState(TokenState tokenState) {
            this.tokenState = tokenState;
            return this;
        }
        
        public Builder attemptState(AttemptState attemptState) {
            this.attemptState = attemptState;
            return this;
        }
        
        // Convenience methods for backward compatibility
        private AuthenticationSession.Builder sessionBuilder;
        private TokenState.Builder tokenStateBuilder;
        private AttemptState.Builder attemptStateBuilder;
        
        private AuthenticationSession.Builder getSessionBuilder() {
            if (sessionBuilder == null) {
                sessionBuilder = AuthenticationSession.builder();
            }
            return sessionBuilder;
        }
        
        private TokenState.Builder getTokenStateBuilder() {
            if (tokenStateBuilder == null) {
                tokenStateBuilder = TokenState.builder();
            }
            return tokenStateBuilder;
        }
        
        private AttemptState.Builder getAttemptStateBuilder() {
            if (attemptStateBuilder == null) {
                attemptStateBuilder = AttemptState.builder();
            }
            return attemptStateBuilder;
        }
        
        public Builder attemptId(String attemptId) {
            getSessionBuilder().attemptId(attemptId);
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            getSessionBuilder().sessionId(sessionId);
            return this;
        }
        
        public Builder customerIdentifier(CustomerIdentifier customerIdentifier) {
            getSessionBuilder().customerIdentifier(customerIdentifier);
            return this;
        }
        
        public Builder brand(String brand) {
            getSessionBuilder().brand(brand);
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            getSessionBuilder().startTime(startTime);
            return this;
        }
        
        // Convenience methods for TokenState
        public Builder eligibleTokens(List<String> eligibleTokens) {
            getTokenStateBuilder().eligibleTokens(eligibleTokens);
            return this;
        }
        
        public Builder authenticatedTokens(List<String> authenticatedTokens) {
            getTokenStateBuilder().authenticatedTokens(authenticatedTokens);
            return this;
        }
        
        public Builder requiredTokensForFullAuth(List<String> requiredTokensForFullAuth) {
            getTokenStateBuilder().requiredTokensForFullAuth(requiredTokensForFullAuth);
            return this;
        }
        
        public Builder failedTokens(List<String> failedTokens) {
            getTokenStateBuilder().failedTokens(failedTokens);
            return this;
        }
        
        public Builder askedTokens(List<String> askedTokens) {
            getTokenStateBuilder().askedTokens(askedTokens);
            return this;
        }
        
        // Convenience methods for AttemptState
        public Builder tokenAttemptsRemaining(Map<String, Integer> tokenAttemptsRemaining) {
            getAttemptStateBuilder().tokenAttemptsRemaining(tokenAttemptsRemaining);
            return this;
        }
        
        public Builder overallAttemptsRemaining(int overallAttemptsRemaining) {
            getAttemptStateBuilder().overallAttemptsRemaining(overallAttemptsRemaining);
            return this;
        }
        
        public Builder currentStatus(AuthStatus currentStatus) {
            getAttemptStateBuilder().currentStatus(currentStatus);
            return this;
        }
        
        public AuthenticationContext build() {
            // Use provided instances or build from builders
            AuthenticationSession finalSession = session;
            if (finalSession == null) {
                finalSession = sessionBuilder != null ? sessionBuilder.build() : AuthenticationSession.builder().build();
            }
            
            TokenState finalTokenState = tokenState;
            if (finalTokenState == null) {
                finalTokenState = tokenStateBuilder != null ? tokenStateBuilder.build() : TokenState.builder().build();
            }
            
            AttemptState finalAttemptState = attemptState;
            if (finalAttemptState == null) {
                finalAttemptState = attemptStateBuilder != null ? attemptStateBuilder.build() : AttemptState.builder().build();
            }
            
            return new AuthenticationContext(finalSession, finalTokenState, finalAttemptState);
        }
    }
    
    @Override
    public String toString() {
        return String.format("AuthenticationContext{session=%s, status=%s, authenticatedTokens=%s}",
                session, getCurrentStatus(), getAuthenticatedTokens());
    }
} 