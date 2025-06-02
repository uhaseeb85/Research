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
        
        if (trustInfo != null && trustInfo.isLowTrust()) {
            // Low trust - enforce stronger authentication
            
            // Check if we already tried all SSN-based tokens and they failed
            if (context.isTokenFailed("SSN_FULL") && context.isTokenFailed("SSN") && context.isTokenFailed("SSN_LAST_4")) {
                return null; // Give up after all SSN attempts fail - let other rules handle
            }
            
            // Try full SSN first if available and not failed
            if (context.getEligibleTokens().contains("SSN_FULL") && 
                context.canReAskToken("SSN_FULL") && 
                !context.isTokenFailed("SSN_FULL")) {
                return "SSN_FULL";
            }
            
            // If full SSN failed/unavailable, try regular SSN
            if (context.getEligibleTokens().contains("SSN") && 
                context.canReAskToken("SSN") && 
                !context.isTokenFailed("SSN")) {
                return "SSN";
            }
            
            // If regular SSN also failed, try last 4 digits as emergency fallback
            if (context.getEligibleTokens().contains("SSN_LAST_4") && 
                context.canReAskToken("SSN_LAST_4") && 
                !context.isTokenFailed("SSN_LAST_4")) {
                return "SSN_LAST_4";
            }
        }
        
        return null; // Let other rules handle normal trust scenarios or rule exhausted options
    }
    
    @Override
    public String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken) {
        // No special failure handling
        return null;
    }
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        return trustInfo != null && trustInfo.isLowTrust();
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