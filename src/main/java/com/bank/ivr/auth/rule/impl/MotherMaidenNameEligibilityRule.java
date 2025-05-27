package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.EligibilityRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if a customer is eligible for Mother's Maiden Name-based authentication.
 * The customer must have a mother's maiden name on record and their account must be active.
 */
@Component
public class MotherMaidenNameEligibilityRule implements EligibilityRule {
    
    @Override
    public boolean isEligible(CustomerProfile customerProfile, String brand) {
        return customerProfile.getMotherMaidenName() != null 
               && !customerProfile.getMotherMaidenName().trim().isEmpty()
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getTokenName() {
        return "MOTHER_MAIDEN_NAME";
    }
    
    @Override
    public String getRuleName() {
        return "MOTHER_MAIDEN_NAME_ELIGIBILITY";
    }
    
    @Override
    public int getPriority() {
        return 70; // Lower priority than SSN, PIN, and DOB
    }
} 