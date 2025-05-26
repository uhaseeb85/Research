package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;

/**
 * Interface for rules that determine which specific authentication token to ask for next.
 * These rules are evaluated during response building to make intelligent token selection decisions.
 */
public interface TokenSelectionRule {
    
    /**
     * Determines the next token to ask based on current authentication context and customer profile.
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return the token name to ask next, or null if this rule doesn't apply
     */
    String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile);
    
    /**
     * Determines if this rule applies to the current authentication scenario.
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return true if this rule should be evaluated
     */
    boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile);
    
    /**
     * Handles token validation failure scenarios.
     * Determines what to do when a token validation fails.
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
     * Returns the brand this rule applies to, or "DEFAULT" for all brands.
     * 
     * @return the brand code
     */
    default String getBrand() {
        return "DEFAULT";
    }
    
    /**
     * Returns a descriptive name for this rule for logging and debugging.
     * 
     * @return the rule name
     */
    String getRuleName();
    
    /**
     * Returns the priority of this rule. Higher numbers indicate higher priority.
     * Rules with higher priority are evaluated first.
     * 
     * @return the rule priority (default is 0)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Returns a description of the conditions this rule handles.
     * 
     * @return the condition description
     */
    default String getConditionDescription() {
        return "No description provided";
    }
} 