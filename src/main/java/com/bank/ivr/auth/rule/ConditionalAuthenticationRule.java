package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;

/**
 * Enhanced authentication rule interface for complex conditional logic.
 * Supports trust level and phone matching based authentication flows.
 */
public interface ConditionalAuthenticationRule extends AuthenticationRule {
    
    /**
     * Determines the next token to ask based on current context and customer profile.
     * This method handles complex conditional logic including trust levels and phone matching.
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return the token name to ask next, or null if no token should be asked
     */
    String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile);
    
    /**
     * Determines if this rule applies to the current authentication scenario.
     * This allows rules to be conditionally applied based on trust level, phone matching, etc.
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return true if this rule should be applied
     */
    boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile);
    
    /**
     * Handles token validation failure scenarios.
     * Determines what to do when a token validation fails (e.g., wrong SSN digits).
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @param failedToken the token that failed validation
     * @return the next token to ask, or null if authentication should fail
     */
    default String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken) {
        return null; // Default behavior: no fallback token
    }
    
    /**
     * Returns the condition description for this rule for debugging and logging.
     * 
     * @return a human-readable description of when this rule applies
     */
    String getConditionDescription();
} 