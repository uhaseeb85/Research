package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;

/**
 * Interface for authentication rules that determine eligibility and completion criteria.
 * This allows for flexible business rule implementation and easy extension.
 */
public interface AuthenticationRule {
    
    /**
     * Evaluates the rule against the current authentication context and customer profile.
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return true if the rule condition is met, false otherwise
     */
    boolean evaluate(AuthenticationContext context, CustomerProfile customerProfile);
    
    /**
     * Returns a descriptive name for this rule for logging and debugging purposes.
     * 
     * @return the rule name
     */
    String getRuleName();
    
    /**
     * Returns the priority of this rule. Higher numbers indicate higher priority.
     * This can be used when multiple rules need to be ordered.
     * 
     * @return the rule priority (default is 0)
     */
    default int getPriority() {
        return 0;
    }
} 