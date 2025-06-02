package com.bank.ivr.auth.rule.impl;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.TokenSelectionRule;

/**
 * Brand-agnostic rule for high-value customers.
 * Prioritizes enhanced security tokens for customers with high account balances.
 */
@Component("HIGH_VALUE_CUSTOMER_RULE")
public class HighValueCustomerRule implements TokenSelectionRule {
    
    private static final double HIGH_VALUE_THRESHOLD = 100000.0;
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        if (isHighValueCustomer(customerProfile)) {
            // Progressive escalation for high-value customers
            // Start with most convenient, escalate to more secure methods
            
            // First preference: Mobile PIN (convenient and secure)
            if (context.getEligibleTokens().contains("MOBILE_PIN") && 
                context.canReAskToken("MOBILE_PIN") && 
                !context.isTokenFailed("MOBILE_PIN")) {
                return "MOBILE_PIN";
            }
            
            // Second preference: Debit Card PIN (if mobile PIN failed/unavailable)
            if (context.getEligibleTokens().contains("DEBIT_CARD_PIN") && 
                context.canReAskToken("DEBIT_CARD_PIN") && 
                !context.isTokenFailed("DEBIT_CARD_PIN")) {
                return "DEBIT_CARD_PIN";
            }
            
            // Third preference: Voice biometric (premium feature for high-value customers)
            if (context.getEligibleTokens().contains("VOICE_BIOMETRIC") && 
                context.canReAskToken("VOICE_BIOMETRIC") && 
                !context.isTokenFailed("VOICE_BIOMETRIC")) {
                return "VOICE_BIOMETRIC";
            }
            
            // Final fallback: Face ID biometric (if available)
            if (context.getEligibleTokens().contains("FACE_ID") && 
                context.canReAskToken("FACE_ID") && 
                !context.isTokenFailed("FACE_ID")) {
                return "FACE_ID";
            }
            
            // If all high-value preferred methods exhausted, let other rules handle
            // (e.g., trust-based rules might force SSN for security)
        }
        
        return null; // Let other rules or default priority handle non-high-value customers or exhausted options
    }
    
    @Override
    public String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken) {
        // No special failure handling
        return null;
    }
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
        return isHighValueCustomer(customerProfile);
    }
    
    /**
     * Determines if a customer is high-value based on account status and employee ID.
     */
    private boolean isHighValueCustomer(CustomerProfile customerProfile) {
        // Consider customers with employee ID as high-value (employee banking)
        if (customerProfile.getEmployeeId() != null && !customerProfile.getEmployeeId().trim().isEmpty()) {
            return true;
        }
        
        // Consider premium account status as high-value
        String accountStatus = customerProfile.getAccountStatus();
        return "PREMIUM".equals(accountStatus) || "VIP".equals(accountStatus);
    }
    
    @Override
    public int getPriority() {
        return 200; // Default priority, can be overridden by brand
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Applies to all brands by default
    }
    
    @Override
    public String getRuleName() {
        return "HIGH_VALUE_CUSTOMER_RULE";
    }
} 