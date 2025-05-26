package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.EligibilityRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if a customer is eligible for SSN-based authentication.
 * The customer must have an SSN on record and their account must be active.
 */
@Component
public class SsnEligibilityRule implements EligibilityRule {
    
    @Override
    public boolean isEligible(CustomerProfile customerProfile, String brand) {
        return customerProfile.getSsn() != null 
               && !customerProfile.getSsn().trim().isEmpty()
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getTokenName() {
        return "SSN";
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