package com.bank.ivr.auth.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;

/**
 * Brand-specific SSN validator for PREMIUM_BANK.
 * Implements stricter SSN validation requirements with enhanced security logging.
 * Premium customers must provide full 9-digit SSN - no partial matching allowed.
 */
@Component
public class PremiumBankSsnValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(PremiumBankSsnValidator.class);
    
    // Premium Bank specific configuration
    private static final int REQUIRED_SSN_LENGTH = 9;
    private static final double MIN_ACCOUNT_BALANCE_FOR_PREMIUM = 50000.0;
    
    @Override
    public String getTokenName() {
        return "SSN";
    }
    
    @Override
    public String getBrand() {
        return "PREMIUM_BANK"; // Brand-specific validator
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        logger.debug("PREMIUM_BANK SSN validation for customer: {}", customerIdentifierValue);
        
        // Premium Bank specific validations
        if (!isPremiumCustomer(customerProfile)) {
            logger.warn("Customer {} not eligible for PREMIUM_BANK SSN validation", customerIdentifierValue);
            return false;
        }
        
        if (customerProfile.getSsn() == null || providedTokenValue == null) {
            logger.debug("PREMIUM_BANK SSN validation failed: null values for customer {}", customerIdentifierValue);
            auditFailedAttempt(customerIdentifierValue, "NULL_VALUES");
            return false;
        }
        
        String normalizedProvided = normalizeTokenValue(providedTokenValue);
        String storedSsn = customerProfile.getSsn();
        
        // Premium Bank ONLY accepts full SSN - no partial matching
        if (normalizedProvided.length() != REQUIRED_SSN_LENGTH) {
            logger.warn("PREMIUM_BANK SSN validation failed: invalid length {} for customer {}", 
                       normalizedProvided.length(), customerIdentifierValue);
            auditFailedAttempt(customerIdentifierValue, "INVALID_LENGTH");
            return false;
        }
        
        // Full SSN comparison only
        boolean isValid = normalizedProvided.equals(storedSsn);
        
        if (isValid) {
            logger.info("PREMIUM_BANK SSN validation successful for customer {}", customerIdentifierValue);
            auditSuccessfulValidation(customerIdentifierValue, customerProfile);
        } else {
            logger.warn("PREMIUM_BANK SSN validation failed: SSN mismatch for customer {}", customerIdentifierValue);
            auditFailedAttempt(customerIdentifierValue, "SSN_MISMATCH");
        }
        
        return isValid;
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Remove all non-digit characters for Premium Bank
        return providedTokenValue.replaceAll("[^0-9]", "");
    }
    
    @Override
    public int getPriority() {
        return 150; // Higher than default SSN validator
    }
    
    /**
     * Premium Bank specific customer eligibility check.
     * Only high-value customers get Premium Bank treatment.
     */
    private boolean isPremiumCustomer(CustomerProfile profile) {
        // Check account balance for premium status
        if (profile.getAccountBalance() != null && 
            profile.getAccountBalance() >= MIN_ACCOUNT_BALANCE_FOR_PREMIUM) {
            return true;
        }
        
        // Employee accounts are considered premium
        if (profile.getEmployeeId() != null && !profile.getEmployeeId().trim().isEmpty()) {
            return true;
        }
        
        // VIP or Premium account status
        String status = profile.getAccountStatus();
        if ("PREMIUM".equals(status) || "VIP".equals(status)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Enhanced security audit logging for successful Premium Bank SSN validation.
     */
    private void auditSuccessfulValidation(String customerIdentifierValue, CustomerProfile profile) {
        logger.info("PREMIUM_BANK_AUDIT: Successful SSN validation - Customer: {}, Account Balance: {}, Employee ID: {}", 
                   customerIdentifierValue, 
                   profile.getAccountBalance(), 
                   profile.getEmployeeId());
        
        // In real implementation, this would integrate with security audit system
    }
    
    /**
     * Enhanced security audit logging for failed Premium Bank SSN validation attempts.
     */
    private void auditFailedAttempt(String customerIdentifierValue, String failureReason) {
        logger.warn("PREMIUM_BANK_SECURITY_AUDIT: Failed SSN validation attempt - Customer: {}, Reason: {}", 
                   customerIdentifierValue, failureReason);
        
        // In real implementation, this would trigger security monitoring alerts
    }
} 