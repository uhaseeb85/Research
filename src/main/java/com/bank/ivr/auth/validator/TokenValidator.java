package com.bank.ivr.auth.validator;

import com.bank.ivr.auth.model.domain.CustomerProfile;

/**
 * Interface for validating specific token values against customer data.
 * Each token type should have its own implementation.
 */
public interface TokenValidator {
    
    /**
     * Returns the name of the token this validator handles.
     * This should match the token name used in AuthTokenDefinition.
     * 
     * @return the token name (e.g., "SSN", "DEBIT_CARD_PIN")
     */
    String getTokenName();
    
    /**
     * Validates the provided token value against the customer's stored data.
     * 
     * @param customerIdentifierValue the customer identifier (phone, account number, etc.)
     * @param providedTokenValue the token value provided by the customer
     * @param customerProfile the customer's profile containing stored authentication data
     * @return true if the token is valid, false otherwise
     */
    boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile);
    
    /**
     * Returns the priority of this validator when multiple validators might handle the same token.
     * Higher numbers indicate higher priority.
     * 
     * @return the validator priority (default is 0)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Normalizes the provided token value before validation.
     * This can be used to remove formatting, convert to uppercase, etc.
     * Default implementation returns the value as-is.
     * 
     * @param providedTokenValue the raw token value from customer input
     * @return the normalized token value
     */
    default String normalizeTokenValue(String providedTokenValue) {
        return providedTokenValue != null ? providedTokenValue.trim() : null;
    }
} 