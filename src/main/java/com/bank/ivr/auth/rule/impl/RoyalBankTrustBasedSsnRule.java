package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.rule.ConditionalAuthenticationRule;
import org.springframework.stereotype.Component;

/**
 * Royal Bank specific rule for trust-based SSN authentication.
 * Implements complex logic based on trust level and phone matching status.
 */
@Component
public class RoyalBankTrustBasedSsnRule implements ConditionalAuthenticationRule {
    
    private static final String BRAND_CODE = "ROYAL_BANK";
    private static final String SSN_LAST_4 = "SSN_LAST_4";
    private static final String SSN_FULL = "SSN_FULL";
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
        // Only apply to Royal Bank
        if (!BRAND_CODE.equals(context.getBrand())) {
            return false;
        }
        
        // Must have trust level info
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        if (trustInfo == null) {
            return false;
        }
        
        // Customer must have SSN on record
        return customerProfile.getSsn() != null 
               && !customerProfile.getSsn().trim().isEmpty()
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        if (!isApplicable(context, customerProfile)) {
            return null;
        }
        
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        // Scenario 1: Green trust + phone not matched with single SSN -> ask last 4 digits
        if (trustInfo.isHighTrust() && !trustInfo.hasPhoneMatch()) {
            return SSN_LAST_4;
        }
        
        // Scenario 2: Red trust + phone matched with multiple SSNs -> ask full SSN
        if (trustInfo.isLowTrust() && trustInfo.hasMultiplePhoneMatches()) {
            return SSN_FULL;
        }
        
        // Additional scenarios can be added here
        
        // Green trust + single phone match -> ask last 4 digits (lower risk)
        if (trustInfo.isHighTrust() && trustInfo.hasSinglePhoneMatch()) {
            return SSN_LAST_4;
        }
        
        // Red trust + no phone match -> ask full SSN (higher risk)
        if (trustInfo.isLowTrust() && !trustInfo.hasPhoneMatch()) {
            return SSN_FULL;
        }
        
        // Red trust + single phone match -> ask full SSN (still higher risk due to red trust)
        if (trustInfo.isLowTrust() && trustInfo.hasSinglePhoneMatch()) {
            return SSN_FULL;
        }
        
        // Default fallback
        return SSN_LAST_4;
    }
    
    @Override
    public String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken) {
        if (!isApplicable(context, customerProfile)) {
            return null;
        }
        
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        // Scenario 3: Green trust + phone not matched + last 4 SSN failed -> ask full SSN
        if (SSN_LAST_4.equals(failedToken) && trustInfo.isHighTrust() && !trustInfo.hasPhoneMatch()) {
            return SSN_FULL;
        }
        
        // If full SSN failed, no more fallback options
        if (SSN_FULL.equals(failedToken)) {
            return null;
        }
        
        // For any other last 4 failure, escalate to full SSN
        if (SSN_LAST_4.equals(failedToken)) {
            return SSN_FULL;
        }
        
        return null;
    }
    
    @Override
    public boolean evaluate(AuthenticationContext context, CustomerProfile customerProfile) {
        // This rule is for token selection, not eligibility
        return isApplicable(context, customerProfile);
    }
    
    @Override
    public String getRuleName() {
        return "ROYAL_BANK_TRUST_BASED_SSN";
    }
    
    @Override
    public String getConditionDescription() {
        return "Royal Bank trust level and phone matching based SSN authentication rule. " +
               "Determines whether to ask for last 4 digits or full SSN based on trust level (RED/GREEN) " +
               "and phone number matching status (not matched/single match/multiple matches).";
    }
    
    @Override
    public int getPriority() {
        return 200; // High priority for brand-specific rules
    }
} 