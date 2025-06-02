package com.bank.ivr.auth.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.util.EncryptionUtil;
import com.bank.ivr.auth.validator.TokenValidator;

/**
 * Brand-specific PIN validator for COMMUNITY_BANK.
 * Implements community-friendly PIN validation with flexible requirements
 * and enhanced customer service features for local community members.
 */
@Component
public class CommunityBankPinValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(CommunityBankPinValidator.class);
    
    // Community Bank specific configuration - more lenient
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 6; // Allow both 4 and 6 digit PINs
    
    @Override
    public String getTokenName() {
        return "DEBIT_CARD_PIN";
    }
    
    @Override
    public String getBrand() {
        return "COMMUNITY_BANK"; // Brand-specific validator
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        logger.debug("COMMUNITY_BANK PIN validation for customer: {}", customerIdentifierValue);
        
        if (customerProfile.getHashedPin() == null || providedTokenValue == null) {
            logger.debug("COMMUNITY_BANK PIN validation failed: null values for customer {}", customerIdentifierValue);
            return false;
        }
        
        String normalizedProvided = normalizeTokenValue(providedTokenValue);
        
        // Community Bank allows flexible PIN formats
        if (!isValidCommunityBankPinFormat(normalizedProvided)) {
            logger.debug("COMMUNITY_BANK PIN validation failed: invalid format for customer {} (length: {})", 
                        customerIdentifierValue, normalizedProvided.length());
            return false;
        }
        
        try {
            // Standard PIN verification using encryption utility
            boolean isValid = EncryptionUtil.verify(normalizedProvided, customerProfile.getHashedPin());
            
            if (isValid) {
                logger.info("COMMUNITY_BANK PIN validation successful for customer {}", customerIdentifierValue);
                logCommunityBankSuccess(customerIdentifierValue, customerProfile);
            } else {
                logger.debug("COMMUNITY_BANK PIN validation failed for customer {}", customerIdentifierValue);
                logCommunityBankFailure(customerIdentifierValue);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("COMMUNITY_BANK PIN validation error for customer {}: {}", customerIdentifierValue, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Community Bank is flexible - remove all non-digit characters and trim
        return providedTokenValue.replaceAll("[^0-9]", "").trim();
    }
    
    @Override
    public int getPriority() {
        return 85; // Slightly lower than default to allow brand override
    }
    
    /**
     * Community Bank allows flexible PIN formats - both 4 and 6 digit PINs.
     * This accommodates customers who may have different PIN preferences.
     */
    private boolean isValidCommunityBankPinFormat(String pin) {
        if (pin == null) {
            return false;
        }
        
        int length = pin.length();
        boolean isValidLength = length >= MIN_PIN_LENGTH && length <= MAX_PIN_LENGTH;
        boolean isAllDigits = pin.matches("^\\d+$");
        
        return isValidLength && isAllDigits;
    }
    
    /**
     * Community-friendly success logging with helpful context.
     */
    private void logCommunityBankSuccess(String customerIdentifierValue, CustomerProfile profile) {
        // Community Bank tracks customer interaction patterns for better service
        logger.info("COMMUNITY_BANK: Successful PIN validation for local customer {} (Account: {})", 
                   customerIdentifierValue, 
                   profile.getAccountStatus());
        
        // In real implementation, this might update customer service records
    }
    
    /**
     * Community-friendly failure logging with customer service focus.
     */
    private void logCommunityBankFailure(String customerIdentifierValue) {
        // Community Bank focuses on customer service rather than security alerts
        logger.debug("COMMUNITY_BANK: PIN validation unsuccessful for customer {} - customer service follow-up may be needed", 
                    customerIdentifierValue);
        
        // In real implementation, this might trigger customer service outreach
    }
} 