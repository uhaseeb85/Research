package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing token validation operations.
 * Acts as a facade for all token validators and provides efficient lookup.
 */
@Service
public class TokenValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationService.class);
    
    private final Map<String, TokenValidator> validatorMap;
    
    @Autowired
    public TokenValidationService(List<TokenValidator> validators) {
        // Create a map for efficient validator lookup by token name
        this.validatorMap = validators.stream()
                .collect(Collectors.toMap(
                        TokenValidator::getTokenName,
                        Function.identity(),
                        (existing, replacement) -> {
                            // If there are multiple validators for the same token, choose the one with higher priority
                            if (existing.getPriority() >= replacement.getPriority()) {
                                logger.warn("Multiple validators found for token '{}'. Using validator with priority {}",
                                          existing.getTokenName(), existing.getPriority());
                                return existing;
                            } else {
                                logger.warn("Multiple validators found for token '{}'. Using validator with priority {}",
                                          replacement.getTokenName(), replacement.getPriority());
                                return replacement;
                            }
                        }
                ));
        
        logger.info("Initialized TokenValidationService with {} validators: {}", 
                   validatorMap.size(), validatorMap.keySet());
    }
    
    /**
     * Validates a token value against the customer's stored data.
     * 
     * @param tokenName the name of the token to validate
     * @param customerIdentifierValue the customer identifier value
     * @param providedTokenValue the token value provided by the customer
     * @param customerProfile the customer's profile data
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String tokenName, String customerIdentifierValue, 
                               String providedTokenValue, CustomerProfile customerProfile) {
        
        logger.debug("Validating token '{}' for customer '{}'", tokenName, customerIdentifierValue);
        
        TokenValidator validator = validatorMap.get(tokenName);
        if (validator == null) {
            logger.warn("No validator found for token '{}'", tokenName);
            return false;
        }
        
        try {
            boolean isValid = validator.validate(customerIdentifierValue, providedTokenValue, customerProfile);
            logger.debug("Token '{}' validation result: {} for customer '{}'", 
                        tokenName, isValid, customerIdentifierValue);
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating token '{}' for customer '{}': {}", 
                        tokenName, customerIdentifierValue, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if a validator exists for the given token name.
     * 
     * @param tokenName the token name to check
     * @return true if a validator exists, false otherwise
     */
    public boolean hasValidator(String tokenName) {
        return validatorMap.containsKey(tokenName);
    }
    
    /**
     * Gets the validator for the given token name.
     * 
     * @param tokenName the token name
     * @return the validator, or null if not found
     */
    public TokenValidator getValidator(String tokenName) {
        return validatorMap.get(tokenName);
    }
    
    /**
     * Gets all available token names that have validators.
     * 
     * @return a set of token names
     */
    public java.util.Set<String> getSupportedTokenNames() {
        return validatorMap.keySet();
    }
    
    /**
     * Normalizes a token value using the appropriate validator.
     * 
     * @param tokenName the token name
     * @param tokenValue the raw token value
     * @return the normalized token value, or the original value if no validator is found
     */
    public String normalizeTokenValue(String tokenName, String tokenValue) {
        TokenValidator validator = validatorMap.get(tokenName);
        if (validator != null) {
            return validator.normalizeTokenValue(tokenValue);
        }
        return tokenValue;
    }
} 