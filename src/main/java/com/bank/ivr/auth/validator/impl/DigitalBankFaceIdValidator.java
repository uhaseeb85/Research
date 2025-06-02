package com.bank.ivr.auth.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;

/**
 * Brand-specific Face ID validator for DIGITAL_BANK.
 * Implements advanced biometric validation with enhanced security features
 * specific to Digital Bank's mobile-first customer experience.
 */
@Component
public class DigitalBankFaceIdValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DigitalBankFaceIdValidator.class);
    
    // Digital Bank specific configuration
    private static final double FACE_ID_THRESHOLD = 0.95; // Higher threshold than standard
    private static final int MIN_CUSTOMER_AGE = 18; // Legal requirement for biometrics
    
    @Override
    public String getTokenName() {
        return "FACE_ID";
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK"; // Brand-specific validator
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        logger.debug("DIGITAL_BANK Face ID validation for customer: {}", customerIdentifierValue);
        
        // Digital Bank specific validations
        if (!isEligibleForFaceId(customerProfile)) {
            logger.warn("Customer {} not eligible for Face ID authentication", customerIdentifierValue);
            return false;
        }
        
        if (customerProfile.getFaceIdHash() == null || providedTokenValue == null) {
            logger.debug("Face ID validation failed: missing biometric data for customer {}", customerIdentifierValue);
            return false;
        }
        
        String normalizedProvided = normalizeTokenValue(providedTokenValue);
        
        try {
            // Digital Bank uses advanced biometric comparison with higher threshold
            double similarity = calculateBiometricSimilarity(normalizedProvided, customerProfile.getFaceIdHash());
            
            boolean isValid = similarity >= FACE_ID_THRESHOLD;
            
            if (isValid) {
                logger.info("DIGITAL_BANK Face ID validation successful for customer {} (similarity: {})", 
                           customerIdentifierValue, similarity);
                
                // Update last biometric authentication timestamp for Digital Bank analytics
                updateLastBiometricAuth(customerProfile);
            } else {
                logger.warn("DIGITAL_BANK Face ID validation failed for customer {} (similarity: {}, required: {})", 
                           customerIdentifierValue, similarity, FACE_ID_THRESHOLD);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("DIGITAL_BANK Face ID validation error for customer {}: {}", customerIdentifierValue, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Digital Bank expects uppercase hex format for Face ID hashes
        return providedTokenValue.toUpperCase().replaceAll("[^A-F0-9]", "");
    }
    
    @Override
    public int getPriority() {
        return 200; // Highest priority for Digital Bank's preferred auth method
    }
    
    /**
     * Digital Bank specific eligibility check for Face ID authentication.
     * More restrictive than default implementation.
     */
    private boolean isEligibleForFaceId(CustomerProfile profile) {
        // Age verification (Digital Bank policy)
        if (profile.getAge() == null || profile.getAge() < MIN_CUSTOMER_AGE) {
            logger.debug("Customer not eligible: age requirement not met");
            return false;
        }
        
        // Account must be active (Digital Bank requirement)
        if (!"ACTIVE".equals(profile.getAccountStatus())) {
            logger.debug("Customer not eligible: account not active");
            return false;
        }
        
        // Digital Bank requires mobile app enrollment for Face ID
        if (profile.getMobileAppEnrolled() == null || !profile.getMobileAppEnrolled()) {
            logger.debug("Customer not eligible: mobile app not enrolled");
            return false;
        }
        
        return true;
    }
    
    /**
     * Simulates advanced biometric comparison algorithm.
     * In real implementation, this would call Digital Bank's biometric service.
     */
    private double calculateBiometricSimilarity(String providedHash, String storedHash) {
        // Simplified simulation - in reality this would be complex biometric comparison
        if (providedHash.equals(storedHash)) {
            return 1.0; // Perfect match
        }
        
        // Simulate partial similarity based on hash differences
        int differences = 0;
        int minLength = Math.min(providedHash.length(), storedHash.length());
        
        for (int i = 0; i < minLength; i++) {
            if (providedHash.charAt(i) != storedHash.charAt(i)) {
                differences++;
            }
        }
        
        // Calculate similarity percentage
        double similarity = 1.0 - ((double) differences / minLength);
        return Math.max(0.0, similarity);
    }
    
    /**
     * Updates biometric authentication timestamp for Digital Bank analytics.
     */
    private void updateLastBiometricAuth(CustomerProfile profile) {
        // In real implementation, this would update the customer's last biometric auth timestamp
        logger.debug("Updated last biometric authentication timestamp for Digital Bank customer");
    }
} 