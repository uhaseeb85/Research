package com.bank.ivr.auth.rule.impl;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.rule.TokenSelectionRule;

/**
 * Brand-agnostic rule that enforces security based on trust level.
 * Low trust customers are forced to use full SSN authentication.
 */
@Component("TRUST_BASED_SECURITY_RULE")
public class TrustBasedSecurityRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        if (trustInfo != null && "RED".equals(trustInfo.getTrustLevel())) {
            // Low trust - force full SSN authentication
            if (context.getEligibleTokens().contains("SSN_FULL") && 
                context.canReAskToken("SSN_FULL")) {
                return "SSN_FULL";
            }
            // Fallback to SSN if full not available
            if (context.getEligibleTokens().contains("SSN") && 
                context.canReAskToken("SSN")) {
                return "SSN";
            }
        }
        
        return null; // Let other rules handle normal trust scenarios
    }
    
    @Override
    public String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken) {
        // No special failure handling
        return null;
    }
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        return trustInfo != null && "RED".equals(trustInfo.getTrustLevel());
    }
    
    @Override
    public int getPriority() {
        return 250; // Default priority, can be overridden by brand
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Applies to all brands by default
    }
    
    @Override
    public String getRuleName() {
        return "TRUST_BASED_SECURITY_RULE";
    }
} 