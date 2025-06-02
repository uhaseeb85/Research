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
            // Prefer mobile PIN for high-value customers (more secure than basic tokens)
            if (context.getEligibleTokens().contains("MOBILE_PIN") && 
                context.canReAskToken("MOBILE_PIN")) {
                return "MOBILE_PIN";
            }
            
            // Fallback to debit card PIN
            if (context.getEligibleTokens().contains("DEBIT_CARD_PIN") && 
                context.canReAskToken("DEBIT_CARD_PIN")) {
                return "DEBIT_CARD_PIN";
            }
        }
        
        return null; // Let other rules or default priority handle
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