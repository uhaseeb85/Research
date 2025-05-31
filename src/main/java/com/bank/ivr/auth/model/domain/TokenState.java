package com.bank.ivr.auth.model.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Manages the state of tokens during authentication.
 * Extracted from AuthenticationContext to improve separation of concerns.
 */
public class TokenState {
    
    @JsonProperty("eligibleTokens")
    private List<String> eligibleTokens;
    
    @JsonProperty("authenticatedTokens")
    private List<String> authenticatedTokens;
    
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
            @JsonProperty("failedTokens") List<String> failedTokens,
            @JsonProperty("askedTokens") List<String> askedTokens,
            @JsonProperty("lastAskedToken") String lastAskedToken,
            @JsonProperty("askedTokensWithValidationFailure") Map<String, Integer> askedTokensWithValidationFailure) {
        this.eligibleTokens = eligibleTokens != null ? eligibleTokens : new ArrayList<>();
        this.authenticatedTokens = authenticatedTokens != null ? authenticatedTokens : new ArrayList<>();
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
    /**
     * Adds a token to the list of successfully authenticated tokens.
     * 
     * This method is called when a token has been provided by the user
     * and successfully validated against the customer's stored data.
     * 
     * Authenticated tokens indicate successful validation and contribute
     * to the overall authentication completion criteria.
     * 
     * @param tokenName the name of the token that was successfully validated
     */
    public void addAuthenticatedToken(String tokenName) {
        if (!authenticatedTokens.contains(tokenName)) {
            authenticatedTokens.add(tokenName);
        }
    }
    
    /**
     * Adds a token to the list of completely failed tokens due to attempt exhaustion.
     * 
     * This method is called when a token has used up all its allowed attempts
     * and is no longer available for authentication in this session.
     * 
     * IMPORTANT: This is for attempt exhaustion, NOT individual validation failures.
     * Individual validation failures are tracked by markAskedTokenValidationFailure().
     * 
     * A token is added to failed tokens when:
     * - Token has zero attempts remaining after failed validation
     * - DNIS configuration blocks the token permanently
     * - System configuration limits are exceeded
     * 
     * @param tokenName the name of the token that has exhausted all attempts
     */
    public void addFailedToken(String tokenName) {
        if (!failedTokens.contains(tokenName)) {
            failedTokens.add(tokenName);
        }
    }
    
    /**
     * Checks if a token has been successfully authenticated.
     * 
     * This method returns true if the token has been provided by the user
     * and successfully validated against the customer's stored data.
     * 
     * Authenticated tokens contribute to meeting the authentication requirements
     * and are included in successful authentication responses.
     * 
     * @param tokenName the name of the token to check
     * @return true if token has been successfully validated, false otherwise
     */
    public boolean isTokenAuthenticated(String tokenName) {
        return authenticatedTokens.contains(tokenName);
    }
    
    /**
     * Checks if a token has completely failed due to exhausting all allowed attempts.
     * 
     * This method tracks tokens that have used up all their retry attempts and are
     * no longer available for authentication in this session.
     * 
     * IMPORTANT: This method checks attempt exhaustion, NOT validation failures.
     * A token is considered "failed" when it has zero attempts remaining.
     * 
     * A token gets added to failed tokens when:
     * - Token validation failed AND no attempts remaining for this token
     * - DNIS strict mode blocks the token and no retries allowed
     * - Token is rejected due to configuration limits
     * 
     * This is different from validation failures tracked by canReAskToken().
     * 
     * @param tokenName the name of the token to check
     * @return true if token has exhausted all attempts (completely failed),
     *         false if token still has attempts available
     */
    public boolean isTokenFailed(String tokenName) {
        return failedTokens.contains(tokenName);
    }
    
    /**
     * Adds a token to the list of tokens that have been asked from the user.
     * 
     * This method tracks which tokens the system has explicitly requested
     * from the customer during the authentication flow, regardless of whether
     * the user actually provided a value for the token.
     * 
     * Asked tokens are used for:
     * - Preventing duplicate requests for the same token
     * - Smart re-asking logic in combination with validation failures
     * - Tracking authentication flow progression
     * 
     * Note: A token being "asked" does not mean the user provided it.
     * Use validation failure tracking to determine if user provided incorrect values.
     * 
     * @param tokenName the name of the token that was requested from the user
     */
    public void addAskedToken(String tokenName) {
        if (!askedTokens.contains(tokenName)) {
            askedTokens.add(tokenName);
        }
    }
    
    /**
     * Checks if a token has been requested from the user during this authentication session.
     * 
     * This method returns true if the system has explicitly asked the user to provide
     * this token at some point during the authentication flow.
     * 
     * IMPORTANT: This only tracks whether the token was REQUESTED, not whether:
     * - The user actually provided a value
     * - The provided value was correct or incorrect
     * - The token was successfully validated
     * 
     * Use other methods to check validation status:
     * - hasAskedTokenValidationFailure() for incorrect values
     * - isTokenAuthenticated() for successful validation
     * 
     * @param tokenName the name of the token to check
     * @return true if token has been requested from user, false otherwise
     */
    public boolean isTokenAlreadyAsked(String tokenName) {
        return askedTokens.contains(tokenName);
    }
    
    /**
     * Records that a user provided an incorrect value for a specifically requested token.
     * 
     * This method is called when:
     * 1. System asked for a specific token (lastAskedToken)
     * 2. User provided a value for that exact token
     * 3. The provided value failed validation (was incorrect)
     * 
     * This tracking enables "smart re-asking logic" to avoid repeatedly asking
     * for tokens that users have already attempted unsuccessfully.
     * 
     * IMPORTANT: This is different from attempt exhaustion (addFailedToken).
     * This tracks individual validation failures, while addFailedToken tracks
     * when all attempts are used up.
     * 
     * The failure count is incremented each time, allowing tracking of multiple
     * incorrect attempts for the same token.
     * 
     * @param tokenName the name of the token that failed validation
     */
    public void markAskedTokenValidationFailure(String tokenName) {
        askedTokensWithValidationFailure.put(tokenName, 
                askedTokensWithValidationFailure.getOrDefault(tokenName, 0) + 1);
    }
    
    /**
     * Checks if a user has provided incorrect values for a requested token.
     * 
     * This method returns true if the user has provided an incorrect value
     * for a token that the system specifically requested.
     * 
     * This is used by the smart re-asking logic to determine which tokens
     * should be avoided in future requests to prevent user frustration.
     * 
     * IMPORTANT: This checks validation failures, NOT attempt exhaustion.
     * A token can have validation failures but still have attempts remaining.
     * Use isTokenFailed() to check for attempt exhaustion.
     * 
     * @param tokenName the name of the token to check
     * @return true if user provided incorrect value for this token, false otherwise
     */
    public boolean hasAskedTokenValidationFailure(String tokenName) {
        return askedTokensWithValidationFailure.containsKey(tokenName) && 
               askedTokensWithValidationFailure.get(tokenName) > 0;
    }
    
    /**
     * Gets the number of times a user provided incorrect values for a requested token.
     * 
     * This method returns the count of validation failures for a specific token
     * where the user provided an incorrect value in response to a system request.
     * 
     * The count is incremented each time markAskedTokenValidationFailure() is called
     * for the same token, allowing tracking of multiple incorrect attempts.
     * 
     * This information can be used for:
     * - Analytics and user behavior analysis
     * - Progressive security measures
     * - Debugging authentication flows
     * 
     * @param tokenName the name of the token to check
     * @return the number of validation failures for this token (0 if no failures)
     */
    public int getAskedTokenValidationFailureCount(String tokenName) {
        return askedTokensWithValidationFailure.getOrDefault(tokenName, 0);
    }
    
    /**
     * Determines if a token should be avoided for re-asking due to user providing incorrect values.
     * 
     * This method implements "smart re-asking logic" to prevent repeatedly asking for tokens
     * that customers have already attempted but provided incorrect values for.
     * 
     * IMPORTANT: This method does NOT check attempt limits or eligibility. It specifically
     * tracks whether the user has provided an incorrect value for this token.
     * 
     * Returns false (cannot re-ask) when:
     * - System asked for the token
     * - User provided a value for the token  
     * - The provided value failed validation (was incorrect)
     * 
     * Returns true (can ask/re-ask) when:
     * - Token has never been asked
     * - Token was asked but user didn't provide any value
     * - Token was asked and user provided correct value
     * 
     * @param tokenName the name of the token to check
     * @return false if user provided incorrect value (should avoid re-asking), 
     *         true if token can be asked/re-asked
     */
    public boolean canReAskToken(String tokenName) {
        // Smart re-asking logic: don't re-ask tokens that have validation failures
        // If a user provided a token and it failed validation, never re-ask it
        if (hasAskedTokenValidationFailure(tokenName)) {
            return false;
        }
        
        // If no validation failure, can re-ask
        return true;
    }
    
    /**
     * Resets the list of asked tokens for a new authentication attempt.
     * 
     * This method clears the askedTokens list to allow the system to ask
     * for tokens again in a new attempt or authentication round.
     * 
     * IMPORTANT: This method ONLY clears the asked tokens list. It preserves:
     * - Authenticated tokens (successful validations)
     * - Failed tokens (attempt exhaustion)
     * - Validation failure history (smart re-asking logic)
     * - Last asked token tracking
     * 
     * This selective reset allows the system to start fresh with token requests
     * while maintaining the security and user experience benefits of tracking
     * previous validation failures and successes.
     * 
     * Typically called when starting a new authentication round or attempt.
     */
    public void resetAskedTokensForNewAttempt() {
        askedTokens.clear();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<String> eligibleTokens = new ArrayList<>();
        private List<String> authenticatedTokens = new ArrayList<>();
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
            return new TokenState(eligibleTokens, authenticatedTokens,
                    failedTokens, askedTokens, lastAskedToken, askedTokensWithValidationFailure);
        }
    }
} 