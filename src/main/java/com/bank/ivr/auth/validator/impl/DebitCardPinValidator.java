package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.util.EncryptionUtil;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validator for Debit Card PIN tokens.
 * Uses secure hashing to validate the provided PIN against the stored hash.
 * This is a default implementation that works for all brands.
 */
@Component
public class DebitCardPinValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DebitCardPinValidator.class);
    
    @Override
    public String getTokenName() {
        return "DEBIT_CARD_PIN";
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT";
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        if (customerProfile.getHashedPin() == null || providedTokenValue == null) {
            logger.debug("PIN validation failed: null values");
            return false;
        }
        
        String normalizedProvided = normalizeTokenValue(providedTokenValue);
        
        // Validate PIN format (should be 4 digits)
        if (!isValidPinFormat(normalizedProvided)) {
            logger.debug("PIN validation failed: invalid format for customer {}", customerIdentifierValue);
            return false;
        }
        
        try {
            // Use encryption utility to verify the PIN against the stored hash
            boolean isValid = EncryptionUtil.verify(normalizedProvided, customerProfile.getHashedPin());
            
            if (isValid) {
                logger.debug("PIN validation successful for customer {}", customerIdentifierValue);
            } else {
                logger.debug("PIN validation failed for customer {}", customerIdentifierValue);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating PIN for customer {}: {}", customerIdentifierValue, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Remove all non-digit characters and trim
        return providedTokenValue.replaceAll("[^0-9]", "");
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority but lower than SSN
    }
    
    /**
     * Validates that the PIN is in the correct format (4 digits).
     * 
     * @param pin the normalized PIN to validate
     * @return true if the PIN format is valid, false otherwise
     */
    private boolean isValidPinFormat(String pin) {
        return pin != null && pin.matches("^\\d{4}$");
    }
} 