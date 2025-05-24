package com.bank.ivr.auth.service;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing brand-specific authentication configurations.
 * Provides brand-aware token definitions, rules, and authentication requirements.
 */
@Service
public class BrandAuthConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(BrandAuthConfigurationService.class);
    
    private final Map<String, BrandAuthConfiguration> brandConfigurations;
    private final BrandAuthConfiguration defaultConfiguration;
    
    @Autowired
    public BrandAuthConfigurationService(List<BrandAuthConfiguration> configurations) {
        // Create a map for efficient brand lookup
        this.brandConfigurations = configurations.stream()
                .collect(Collectors.toMap(
                        BrandAuthConfiguration::getBrandCode,
                        Function.identity(),
                        (existing, replacement) -> {
                            // If there are multiple configs for the same brand, choose the one with higher priority
                            if (existing.getPriority() >= replacement.getPriority()) {
                                logger.warn("Multiple configurations found for brand '{}'. Using config with priority {}",
                                          existing.getBrandCode(), existing.getPriority());
                                return existing;
                            } else {
                                logger.warn("Multiple configurations found for brand '{}'. Using config with priority {}",
                                          replacement.getBrandCode(), replacement.getPriority());
                                return replacement;
                            }
                        }
                ));
        
        // Set default configuration (can be overridden by setting a brand with code "DEFAULT")
        this.defaultConfiguration = brandConfigurations.getOrDefault("DEFAULT", 
                brandConfigurations.values().stream()
                        .min(Comparator.comparingInt(BrandAuthConfiguration::getPriority))
                        .orElse(null));
        
        logger.info("Initialized BrandAuthConfigurationService with {} brand configurations: {}", 
                   brandConfigurations.size(), brandConfigurations.keySet());
    }
    
    /**
     * Gets the authentication configuration for a specific brand.
     * 
     * @param brandCode the brand code
     * @return the brand configuration or default if not found
     */
    public BrandAuthConfiguration getBrandConfiguration(String brandCode) {
        BrandAuthConfiguration config = brandConfigurations.get(brandCode);
        if (config == null) {
            logger.warn("No configuration found for brand '{}', using default configuration", brandCode);
            return defaultConfiguration;
        }
        return config;
    }
    
    /**
     * Gets brand-specific token definitions with correct priorities.
     * 
     * @param brandCode the brand code
     * @return list of token definitions for the brand
     */
    public List<AuthTokenDefinition> getTokenDefinitionsForBrand(String brandCode) {
        BrandAuthConfiguration config = getBrandConfiguration(brandCode);
        if (config == null) {
            logger.error("No configuration available for brand '{}' and no default configuration", brandCode);
            return Collections.emptyList();
        }
        
        List<AuthTokenDefinition> definitions = config.getTokenDefinitions();
        logger.debug("Retrieved {} token definitions for brand '{}'", definitions.size(), brandCode);
        return definitions;
    }
    
    /**
     * Gets required tokens for a specific brand.
     * 
     * @param brandCode the brand code
     * @return list of required token names
     */
    public List<String> getRequiredTokensForBrand(String brandCode) {
        BrandAuthConfiguration config = getBrandConfiguration(brandCode);
        if (config == null) {
            return Collections.emptyList();
        }
        return config.getRequiredTokens();
    }
    
    /**
     * Gets maximum overall attempts for a specific brand.
     * 
     * @param brandCode the brand code
     * @return maximum overall attempts
     */
    public int getMaxOverallAttemptsForBrand(String brandCode) {
        BrandAuthConfiguration config = getBrandConfiguration(brandCode);
        if (config == null) {
            return 5; // Default fallback
        }
        return config.getMaxOverallAttempts();
    }
    
    /**
     * Gets brand-specific token attempt limits.
     * 
     * @param brandCode the brand code
     * @return map of token name to max attempts
     */
    public Map<String, Integer> getBrandSpecificTokenAttempts(String brandCode) {
        BrandAuthConfiguration config = getBrandConfiguration(brandCode);
        if (config == null) {
            return Collections.emptyMap();
        }
        return config.getBrandSpecificTokenAttempts();
    }
    
    /**
     * Checks if concurrent token authentication is allowed for a brand.
     * 
     * @param brandCode the brand code
     * @return true if concurrent authentication is allowed
     */
    public boolean isConcurrentTokenAuthAllowed(String brandCode) {
        BrandAuthConfiguration config = getBrandConfiguration(brandCode);
        if (config == null) {
            return false; // Default to false for security
        }
        return config.isConcurrentTokenAuthAllowed();
    }
    
    /**
     * Gets brand-specific messages.
     * 
     * @param brandCode the brand code
     * @param messageKey the message key
     * @return the brand-specific message or a default message
     */
    public String getBrandMessage(String brandCode, String messageKey) {
        BrandAuthConfiguration config = getBrandConfiguration(brandCode);
        if (config == null) {
            return getDefaultMessage(messageKey);
        }
        
        String message = config.getBrandMessages().get(messageKey);
        if (message == null) {
            logger.debug("No brand-specific message found for key '{}' and brand '{}', using default", 
                        messageKey, brandCode);
            return getDefaultMessage(messageKey);
        }
        return message;
    }
    
    /**
     * Gets all available brand codes.
     * 
     * @return set of available brand codes
     */
    public Set<String> getAvailableBrands() {
        return brandConfigurations.keySet();
    }
    
    /**
     * Validates if a brand code is supported.
     * 
     * @param brandCode the brand code to validate
     * @return true if the brand is supported
     */
    public boolean isBrandSupported(String brandCode) {
        return brandConfigurations.containsKey(brandCode);
    }
    
    /**
     * Gets default messages for fallback scenarios.
     * 
     * @param messageKey the message key
     * @return default message
     */
    private String getDefaultMessage(String messageKey) {
        switch (messageKey) {
            case "welcome":
                return "Welcome! Let's verify your identity.";
            case "primary_prompt":
                return "Please provide your {token_description}.";
            case "secondary_prompt":
                return "Thank you. Now please provide your {token_description}.";
            case "success":
                return "Authentication successful.";
            case "failure":
                return "Authentication failed. Please try again or contact support.";
            case "customer_not_found":
                return "Customer not found. Please verify your information.";
            case "session_expired":
                return "Authentication session expired. Please start over.";
            case "system_error":
                return "An error occurred. Please try again.";
            case "no_methods":
                return "No available authentication methods.";
            default:
                return "Please follow the authentication prompts.";
        }
    }
} 