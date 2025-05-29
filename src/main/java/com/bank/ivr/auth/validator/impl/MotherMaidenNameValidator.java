package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validator for Mother's Maiden Name tokens.
 * Performs case-insensitive comparison with normalization.
 * This is a default implementation that works for all brands.
 */
@Component
public class MotherMaidenNameValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(MotherMaidenNameValidator.class);
    
    @Override
    public String getTokenName() {
        return "MOTHER_MAIDEN_NAME";
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT";
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        if (customerProfile.getMotherMaidenName() == null || providedTokenValue == null) {
            logger.debug("Mother maiden name validation failed: null values");
            return false;
        }
        
        String normalizedProvided = normalizeTokenValue(providedTokenValue);
        String normalizedStored = normalizeTokenValue(customerProfile.getMotherMaidenName());
        
        boolean isValid = normalizedProvided.equals(normalizedStored);
        
        logger.debug("Mother maiden name validation {} for customer {}", 
                    isValid ? "successful" : "failed", customerIdentifierValue);
        
        return isValid;
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Convert to uppercase, remove extra spaces, and normalize common variations
        return providedTokenValue.trim()
                .toUpperCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^A-Z\\s'-]", ""); // Keep only letters, spaces, hyphens, and apostrophes
    }
    
    @Override
    public int getPriority() {
        return 70; // Lower priority than SSN, PIN, and DOB
    }
} 