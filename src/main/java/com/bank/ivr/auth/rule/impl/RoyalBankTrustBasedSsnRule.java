package com.bank.ivr.auth.rule.impl;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.rule.TokenSelectionRule;

/**
 * Brand-agnostic rule for trust-based SSN authentication.
 * Implements sophisticated logic based on trust level and phone matching status.
 * Can be configured by any brand that needs trust-level-aware SSN authentication.
 */
@Component("ROYAL_BANK_TRUST_LEVEL_RULE")
public class RoyalBankTrustBasedSsnRule implements TokenSelectionRule {
    
    private static final String SSN_LAST_4 = "SSN_LAST_4";
    private static final String SSN_FULL = "SSN_FULL";
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
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
        
        // Context-aware: Check if we've already tried both SSN options and failed
        if (context.isTokenFailed(SSN_FULL) && context.isTokenFailed(SSN_LAST_4)) {
            return null; // Rule exhausted all options - let other rules handle
        }
        
        // Scenario 1: Green trust + phone not matched with single SSN -> ask last 4 digits
        if (trustInfo.isHighTrust() && !trustInfo.hasPhoneMatch()) {
            if (context.getEligibleTokens().contains(SSN_LAST_4) && 
                context.canReAskToken(SSN_LAST_4) && 
                !context.isTokenFailed(SSN_LAST_4)) {
                return SSN_LAST_4;
            }
        }
        
        // Scenario 2: Red trust + phone matched with multiple SSNs -> ask full SSN
        if (trustInfo.isLowTrust() && trustInfo.hasMultiplePhoneMatches()) {
            if (context.getEligibleTokens().contains(SSN_FULL) && 
                context.canReAskToken(SSN_FULL) && 
                !context.isTokenFailed(SSN_FULL)) {
                return SSN_FULL;
            }
        }
        
        // Green trust + single phone match -> ask last 4 digits (lower risk)
        if (trustInfo.isHighTrust() && trustInfo.hasSinglePhoneMatch()) {
            if (context.getEligibleTokens().contains(SSN_LAST_4) && 
                context.canReAskToken(SSN_LAST_4) && 
                !context.isTokenFailed(SSN_LAST_4)) {
                return SSN_LAST_4;
            }
        }
        
        // Red trust + no phone match -> ask full SSN (higher risk)
        if (trustInfo.isLowTrust() && !trustInfo.hasPhoneMatch()) {
            if (context.getEligibleTokens().contains(SSN_FULL) && 
                context.canReAskToken(SSN_FULL) && 
                !context.isTokenFailed(SSN_FULL)) {
                return SSN_FULL;
            }
        }
        
        // Red trust + single phone match -> ask full SSN (still higher risk due to red trust)
        if (trustInfo.isLowTrust() && trustInfo.hasSinglePhoneMatch()) {
            if (context.getEligibleTokens().contains(SSN_FULL) && 
                context.canReAskToken(SSN_FULL) && 
                !context.isTokenFailed(SSN_FULL)) {
                return SSN_FULL;
            }
        }
        
        // Default fallback - try last 4 if available and not failed
        if (context.getEligibleTokens().contains(SSN_LAST_4) && 
            context.canReAskToken(SSN_LAST_4) && 
            !context.isTokenFailed(SSN_LAST_4)) {
            return SSN_LAST_4;
        }
        
        // Emergency fallback - try full SSN if last 4 not available
        if (context.getEligibleTokens().contains(SSN_FULL) && 
            context.canReAskToken(SSN_FULL) && 
            !context.isTokenFailed(SSN_FULL)) {
            return SSN_FULL;
        }
        
        return null; // Rule exhausted all options
    }
    
    @Override
    public String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken) {
        if (!isApplicable(context, customerProfile)) {
            return null;
        }
        
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        // Context-aware escalation: Only escalate if the higher security token is available and not failed
        
        // Scenario 3: Green trust + phone not matched + last 4 SSN failed -> ask full SSN
        if (SSN_LAST_4.equals(failedToken) && trustInfo.isHighTrust() && !trustInfo.hasPhoneMatch()) {
            if (context.getEligibleTokens().contains(SSN_FULL) && 
                context.canReAskToken(SSN_FULL) && 
                !context.isTokenFailed(SSN_FULL)) {
                return SSN_FULL;
            }
        }
        
        // For any other last 4 failure, escalate to full SSN if available
        if (SSN_LAST_4.equals(failedToken)) {
            if (context.getEligibleTokens().contains(SSN_FULL) && 
                context.canReAskToken(SSN_FULL) && 
                !context.isTokenFailed(SSN_FULL)) {
                return SSN_FULL;
            }
        }
        
        // If full SSN failed, no more escalation options
        return null;
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Brand-agnostic - can be configured by any brand
    }
    
    @Override
    public String getRuleName() {
        return "ROYAL_BANK_TRUST_LEVEL_RULE"; // Match Spring bean name
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