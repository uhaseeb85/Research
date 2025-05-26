package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.CustomerProfile;

/**
 * Interface for rules that determine customer eligibility for specific authentication tokens.
 * These rules are evaluated during initial context creation to determine available authentication methods.
 */
public interface EligibilityRule {
    
    /**
     * Determines if a customer is eligible for a specific authentication token.
     * 
     * @param customerProfile the customer's profile data
     * @param brand the brand code for brand-specific eligibility
     * @return true if the customer is eligible for this token type
     */
    boolean isEligible(CustomerProfile customerProfile, String brand);
    
    /**
     * Returns the token name this rule determines eligibility for.
     * 
     * @return the token name (e.g., "SSN", "DEBIT_CARD_PIN")
     */
    String getTokenName();
    
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
     * Returns the priority of this rule when multiple eligibility rules exist for the same token.
     * Higher numbers indicate higher priority.
     * 
     * @return the rule priority (default is 0)
     */
    default int getPriority() {
        return 0;
    }
} 