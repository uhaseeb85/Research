package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;

/**
 * Interface for rules that determine if additional authentication tokens should be requested
 * after successful validation of a token. These rules are evaluated after token validation
 * succeeds and can trigger additional authentication requirements based on:
 * - Trust levels (RED/GREEN)
 * - Phone number matching status
 * - Customer profile attributes
 * - Risk assessment factors
 * - Brand-specific security policies
 */
public interface PostValidationRule {
    
    /**
     * Determines if additional tokens should be requested after successful validation
     * of the specified token.
     * 
     * @param validatedToken the token that was successfully validated
     * @param context the current authentication context including trust level info
     * @param customerProfile the customer's profile data
     * @return TokenValidationResult indicating if additional tokens are needed
     */
    TokenValidationResult evaluatePostValidation(String validatedToken, 
                                                AuthenticationContext context, 
                                                CustomerProfile customerProfile);
    
    /**
     * Determines if this rule applies to the current authentication scenario.
     * 
     * @param validatedToken the token that was successfully validated
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return true if this rule should be evaluated
     */
    boolean isApplicable(String validatedToken, 
                        AuthenticationContext context, 
                        CustomerProfile customerProfile);
    
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
    
    /**
     * Returns the tokens this rule can trigger additional authentication for.
     * If empty, the rule applies to all tokens.
     * 
     * @return list of token names this rule applies to
     */
    default java.util.List<String> getApplicableTokens() {
        return java.util.Collections.emptyList(); // Empty means applies to all tokens
    }
} 