package com.bank.ivr.auth.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;
import com.bank.ivr.auth.validator.TokenValidator;

/**
 * Service for managing token validation operations.
 * Acts as a facade for all token validators and provides efficient lookup.
 * Enforces the rule that there can only be one validator per token per brand.
 * Enhanced to support post-validation rules that can trigger additional token requests.
 */
@Service
public class TokenValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationService.class);
    
    private final Map<String, TokenValidator> validatorMap;
    private final PostValidationRuleService postValidationRuleService;
    
    @Autowired
    public TokenValidationService(List<TokenValidator> validators, PostValidationRuleService postValidationRuleService) {
        this.postValidationRuleService = postValidationRuleService;
        // Create a map for efficient validator lookup by brand+token combination
        this.validatorMap = new HashMap<>();
        
        // Loop through all validators and build the map with uniqueness validation
        for (TokenValidator validator : validators) {
            String tokenName = validator.getTokenName();
            String brand = validator.getBrand();
            String compositeKey = createCompositeKey(brand, tokenName);
            
            TokenValidator existingValidator = validatorMap.get(compositeKey);
            
            if (existingValidator == null) {
                // No existing validator for this brand+token combination, add it
                validatorMap.put(compositeKey, validator);
                logger.debug("Registered validator for brand '{}', token '{}' with priority {}", 
                            brand, tokenName, validator.getPriority());
            } else {
                // There's already a validator for this brand+token combination - this violates our constraint
                throw new IllegalStateException(
                    String.format("Multiple validators found for brand '%s' and token '%s': " +
                                "existing validator with priority %d, new validator with priority %d. " +
                                "Only one validator per token per brand is allowed.",
                                brand, tokenName, 
                                existingValidator.getPriority(), validator.getPriority()));
            }
        }
        
        logger.info("Initialized TokenValidationService with {} brand-specific validators", validatorMap.size());
        logValidatorMapping();
    }
    
    /**
     * Validates a token value against the customer's stored data for a specific brand.
     * 
     * @param tokenName the name of the token to validate
     * @param brand the brand code
     * @param customerIdentifierValue the customer identifier value
     * @param providedTokenValue the token value provided by the customer
     * @param customerProfile the customer's profile data
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String tokenName, String brand, String customerIdentifierValue, 
                               String providedTokenValue, CustomerProfile customerProfile) {
        
        logger.debug("Validating token '{}' for brand '{}' and customer '{}'", tokenName, brand, customerIdentifierValue);
        
        TokenValidator validator = getValidatorForBrandAndToken(brand, tokenName);
        if (validator == null) {
            logger.warn("No validator found for brand '{}' and token '{}'", brand, tokenName);
            return false;
        }
        
        try {
            boolean isValid = validator.validate(customerIdentifierValue, providedTokenValue, customerProfile);
            logger.debug("Token '{}' validation result: {} for brand '{}' and customer '{}'", 
                        tokenName, isValid, brand, customerIdentifierValue);
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating token '{}' for brand '{}' and customer '{}': {}", 
                        tokenName, brand, customerIdentifierValue, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Enhanced validation method that includes post-validation rule evaluation.
     * This method validates the token and then evaluates post-validation rules to determine
     * if additional tokens should be requested based on trust levels, phone matching, and
     * customer profile attributes.
     * 
     * @param tokenName the name of the token to validate
     * @param brand the brand code
     * @param customerIdentifierValue the customer identifier value
     * @param providedTokenValue the token value provided by the customer
     * @param customerProfile the customer's profile data
     * @param context the authentication context including trust level info
     * @return TokenValidationResult indicating validation success and if additional tokens are needed
     */
    public TokenValidationResult validateTokenWithPostValidation(String tokenName, String brand, 
                                                               String customerIdentifierValue, 
                                                               String providedTokenValue, 
                                                               CustomerProfile customerProfile,
                                                               AuthenticationContext context) {
        
        logger.debug("Validating token '{}' with post-validation rules for brand '{}' and customer '{}'", 
                    tokenName, brand, customerIdentifierValue);
        
        // First perform standard validation
        boolean isValid = validateToken(tokenName, brand, customerIdentifierValue, providedTokenValue, customerProfile);
        
        if (!isValid) {
            logger.debug("Token '{}' validation failed for brand '{}' and customer '{}'", 
                        tokenName, brand, customerIdentifierValue);
            return TokenValidationResult.failure();
        }
        
        // Token is valid, now evaluate post-validation rules
        try {
            TokenValidationResult postValidationResult = postValidationRuleService.evaluatePostValidation(
                    tokenName, context, customerProfile);
            
            logger.debug("Post-validation evaluation for token '{}', brand '{}': requiresAdditional={}, reason='{}'",
                        tokenName, brand, postValidationResult.requiresAdditionalTokens(), 
                        postValidationResult.getReason());
            
            return postValidationResult;
            
        } catch (Exception e) {
            logger.error("Error during post-validation rule evaluation for token '{}', brand '{}', customer '{}': {}", 
                        tokenName, brand, customerIdentifierValue, e.getMessage(), e);
            // If post-validation fails, return simple success to not block authentication
            return TokenValidationResult.success();
        }
    }
    

    
    /**
     * Checks if a validator exists specifically for the given brand and token name.
     * Does NOT include fallback to DEFAULT brand.
     * 
     * @param brand the brand code
     * @param tokenName the token name to check
     * @return true if a validator exists specifically for this brand, false otherwise
     */
    public boolean hasValidator(String brand, String tokenName) {
        return validatorMap.containsKey(createCompositeKey(brand, tokenName));
    }
    

    
    /**
     * Gets the validator specifically for the given brand and token name.
     * Does NOT include fallback to DEFAULT brand.
     * 
     * @param brand the brand code
     * @param tokenName the token name
     * @return the validator for this specific brand, or null if not found
     */
    public TokenValidator getValidator(String brand, String tokenName) {
        return validatorMap.get(createCompositeKey(brand, tokenName));
    }
    

    
    /**
     * Gets all available brand+token combinations that have validators.
     * 
     * @return a set of brand+token composite keys
     */
    public java.util.Set<String> getSupportedBrandTokenCombinations() {
        return validatorMap.keySet();
    }
    
    /**
     * Gets all available token names that have validators for a specific brand.
     * 
     * @param brand the brand code
     * @return a set of token names
     */
    public java.util.Set<String> getSupportedTokenNamesForBrand(String brand) {
        java.util.Set<String> tokenNames = new java.util.HashSet<>();
        for (Map.Entry<String, TokenValidator> entry : validatorMap.entrySet()) {
            if (entry.getKey().startsWith(brand + ":")) {
                String tokenName = extractTokenFromKey(entry.getKey());
                tokenNames.add(tokenName);
            }
        }
        return tokenNames;
    }
    

    
    /**
     * Normalizes a token value using the appropriate validator for a specific brand.
     * 
     * @param brand the brand code
     * @param tokenName the token name
     * @param tokenValue the raw token value
     * @return the normalized token value, or the original value if no validator is found
     */
    public String normalizeTokenValue(String brand, String tokenName, String tokenValue) {
        TokenValidator validator = getValidatorForBrandAndToken(brand, tokenName);
        if (validator != null) {
            return validator.normalizeTokenValue(tokenValue);
        }
        return tokenValue;
    }
    

    
    /**
     * Creates a composite key for brand and token lookup.
     */
    private String createCompositeKey(String brand, String tokenName) {
        return brand + ":" + tokenName;
    }
    
    /**
     * Extracts the token name from a composite key.
     */
    private String extractTokenFromKey(String compositeKey) {
        int colonIndex = compositeKey.indexOf(':');
        return colonIndex >= 0 ? compositeKey.substring(colonIndex + 1) : compositeKey;
    }
    
    /**
     * Gets the validator for a specific brand and token combination.
     * If no brand-specific validator is found, falls back to DEFAULT brand.
     */
    private TokenValidator getValidatorForBrandAndToken(String brand, String tokenName) {
        // First try brand-specific validator
        TokenValidator validator = validatorMap.get(createCompositeKey(brand, tokenName));
        
        // If not found and brand is not DEFAULT, try DEFAULT brand as fallback
        if (validator == null && !"DEFAULT".equals(brand)) {
            validator = validatorMap.get(createCompositeKey("DEFAULT", tokenName));
            if (validator != null) {
                logger.debug("Using DEFAULT validator for brand '{}' and token '{}'", brand, tokenName);
            }
        }
        
        return validator;
    }
    

    
    /**
     * Logs the current validator mapping for debugging purposes.
     */
    private void logValidatorMapping() {
        if (logger.isDebugEnabled()) {
            logger.debug("Validator mapping:");
            for (Map.Entry<String, TokenValidator> entry : validatorMap.entrySet()) {
                String[] parts = entry.getKey().split(":");
                String brand = parts[0];
                String token = parts[1];
                TokenValidator validator = entry.getValue();
                logger.debug("  Brand: {}, Token: {}, Validator: {}, Priority: {}", 
                           brand, token, validator.getClass().getSimpleName(), validator.getPriority());
            }
        }
    }
} 