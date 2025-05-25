package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validator for Social Security Number (SSN) tokens.
 * Supports full SSN matching and last-4-digits partial matching for security.
 * This is a default implementation that works for all brands.
 */
@Component
public class SsnValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(SsnValidator.class);
    
    @Override
    public String getTokenName() {
        return "SSN";
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT";
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        if (customerProfile.getSsn() == null || providedTokenValue == null) {
            logger.debug("SSN validation failed: null values");
            return false;
        }
        
        String normalizedProvided = normalizeTokenValue(providedTokenValue);
        String storedSsn = customerProfile.getSsn();
        
        // Try full SSN match first
        if (normalizedProvided.equals(storedSsn)) {
            logger.debug("SSN validation successful: full match for customer {}", customerIdentifierValue);
            return true;
        }
        
        // Try last 4 digits match (common security practice)
        if (normalizedProvided.length() == 4 && storedSsn.length() >= 4) {
            String lastFourStored = storedSsn.substring(storedSsn.length() - 4);
            if (normalizedProvided.equals(lastFourStored)) {
                logger.debug("SSN validation successful: last-4 match for customer {}", customerIdentifierValue);
                return true;
            }
        }
        
        // Try formatted SSN match (XXX-XX-XXXX)
        String formattedProvided = formatSsn(normalizedProvided);
        if (formattedProvided != null && formattedProvided.equals(storedSsn)) {
            logger.debug("SSN validation successful: formatted match for customer {}", customerIdentifierValue);
            return true;
        }
        
        logger.debug("SSN validation failed for customer {}", customerIdentifierValue);
        return false;
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Remove all non-digit characters
        return providedTokenValue.replaceAll("[^0-9]", "");
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority
    }
    
    /**
     * Formats a 9-digit SSN string into XXX-XX-XXXX format.
     * 
     * @param ssn the unformatted SSN (9 digits)
     * @return the formatted SSN or null if invalid format
     */
    private String formatSsn(String ssn) {
        if (ssn == null || ssn.length() != 9) {
            return null;
        }
        return ssn.substring(0, 3) + "-" + ssn.substring(3, 5) + "-" + ssn.substring(5);
    }
} 