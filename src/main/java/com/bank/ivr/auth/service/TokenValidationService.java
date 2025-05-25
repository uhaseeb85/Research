package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing token validation operations.
 * Acts as a facade for all token validators and provides efficient lookup.
 * Enforces the rule that there can only be one validator per token per brand.
 */
@Service
public class TokenValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationService.class);
    
    private final Map<String, TokenValidator> validatorMap;
    
    @Autowired
    public TokenValidationService(List<TokenValidator> validators) {
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
     * Backward compatibility method for validation without explicit brand.
     * This method will try to find a DEFAULT brand validator first, then any available validator.
     * 
     * @deprecated Use validateToken(String, String, String, String, CustomerProfile) instead
     */
    @Deprecated
    public boolean validateToken(String tokenName, String customerIdentifierValue, 
                               String providedTokenValue, CustomerProfile customerProfile) {
        logger.debug("Using deprecated validateToken method for token '{}' - consider specifying brand", tokenName);
        
        // Try DEFAULT brand first
        TokenValidator validator = getValidatorForBrandAndToken("DEFAULT", tokenName);
        if (validator == null) {
            // Fallback: try to find any validator for this token
            validator = findAnyValidatorForToken(tokenName);
        }
        
        if (validator == null) {
            logger.warn("No validator found for token '{}' (no brand specified)", tokenName);
            return false;
        }
        
        try {
            boolean isValid = validator.validate(customerIdentifierValue, providedTokenValue, customerProfile);
            logger.debug("Token '{}' validation result: {} (no brand specified)", tokenName, isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating token '{}' (no brand specified): {}", tokenName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if a validator exists for the given brand and token name.
     * 
     * @param brand the brand code
     * @param tokenName the token name to check
     * @return true if a validator exists, false otherwise
     */
    public boolean hasValidator(String brand, String tokenName) {
        return validatorMap.containsKey(createCompositeKey(brand, tokenName));
    }
    
    /**
     * Backward compatibility method to check if a validator exists for a token (any brand).
     * 
     * @deprecated Use hasValidator(String, String) instead
     */
    @Deprecated
    public boolean hasValidator(String tokenName) {
        return hasValidator("DEFAULT", tokenName) || findAnyValidatorForToken(tokenName) != null;
    }
    
    /**
     * Gets the validator for the given brand and token name.
     * 
     * @param brand the brand code
     * @param tokenName the token name
     * @return the validator, or null if not found
     */
    public TokenValidator getValidator(String brand, String tokenName) {
        return getValidatorForBrandAndToken(brand, tokenName);
    }
    
    /**
     * Backward compatibility method to get a validator without specifying brand.
     * 
     * @deprecated Use getValidator(String, String) instead
     */
    @Deprecated
    public TokenValidator getValidator(String tokenName) {
        TokenValidator validator = getValidatorForBrandAndToken("DEFAULT", tokenName);
        return validator != null ? validator : findAnyValidatorForToken(tokenName);
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
        return validatorMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(brand + ":"))
                .map(entry -> extractTokenFromKey(entry.getKey()))
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Backward compatibility method to get all supported token names.
     * 
     * @deprecated Use getSupportedTokenNamesForBrand(String) instead
     */
    @Deprecated
    public java.util.Set<String> getSupportedTokenNames() {
        return validatorMap.values().stream()
                .map(TokenValidator::getTokenName)
                .collect(java.util.stream.Collectors.toSet());
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
     * Backward compatibility method for normalizing token values without brand.
     * 
     * @deprecated Use normalizeTokenValue(String, String, String) instead
     */
    @Deprecated
    public String normalizeTokenValue(String tokenName, String tokenValue) {
        TokenValidator validator = getValidatorForBrandAndToken("DEFAULT", tokenName);
        if (validator == null) {
            validator = findAnyValidatorForToken(tokenName);
        }
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
     */
    private TokenValidator getValidatorForBrandAndToken(String brand, String tokenName) {
        return validatorMap.get(createCompositeKey(brand, tokenName));
    }
    
    /**
     * Fallback method to find any validator for a token (used for backward compatibility).
     */
    private TokenValidator findAnyValidatorForToken(String tokenName) {
        return validatorMap.values().stream()
                .filter(validator -> tokenName.equals(validator.getTokenName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Logs the current validator mapping for debugging purposes.
     */
    private void logValidatorMapping() {
        if (logger.isDebugEnabled()) {
            logger.debug("Validator mapping:");
            validatorMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        String[] parts = entry.getKey().split(":");
                        String brand = parts[0];
                        String token = parts[1];
                        TokenValidator validator = entry.getValue();
                        logger.debug("  Brand: {}, Token: {}, Validator: {}, Priority: {}", 
                                   brand, token, validator.getClass().getSimpleName(), validator.getPriority());
                    });
        }
    }
} 