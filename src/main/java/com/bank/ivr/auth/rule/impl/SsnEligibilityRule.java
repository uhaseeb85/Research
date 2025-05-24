package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.AuthenticationRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if a customer is eligible for SSN-based authentication.
 * The customer must have an SSN on record and their account must be active.
 */
@Component
public class SsnEligibilityRule implements AuthenticationRule {
    
    @Override
    public boolean evaluate(AuthenticationContext context, CustomerProfile customerProfile) {
        return customerProfile.getSsn() != null 
               && !customerProfile.getSsn().trim().isEmpty()
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getRuleName() {
        return "SSN_ELIGIBILITY";
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for SSN
    }
} 