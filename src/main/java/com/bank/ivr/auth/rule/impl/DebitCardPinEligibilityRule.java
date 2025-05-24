package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.AuthenticationRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if a customer is eligible for Debit Card PIN-based authentication.
 * The customer must have a PIN on record and their account must be active.
 */
@Component
public class DebitCardPinEligibilityRule implements AuthenticationRule {
    
    @Override
    public boolean evaluate(AuthenticationContext context, CustomerProfile customerProfile) {
        return customerProfile.getHashedPin() != null 
               && !customerProfile.getHashedPin().trim().isEmpty()
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getRuleName() {
        return "DEBIT_CARD_PIN_ELIGIBILITY";
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority but lower than SSN
    }
} 