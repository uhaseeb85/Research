package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.EligibilityRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if a customer is eligible for Date of Birth-based authentication.
 * The customer must have a date of birth on record and their account must be active.
 */
@Component
public class DateOfBirthEligibilityRule implements EligibilityRule {
    
    @Override
    public boolean isEligible(CustomerProfile customerProfile, String brand) {
        return customerProfile.getDateOfBirth() != null 
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getTokenName() {
        return "DATE_OF_BIRTH";
    }
    
    @Override
    public String getRuleName() {
        return "DATE_OF_BIRTH_ELIGIBILITY";
    }
    
    @Override
    public int getPriority() {
        return 80; // Lower priority than SSN and PIN
    }
} 