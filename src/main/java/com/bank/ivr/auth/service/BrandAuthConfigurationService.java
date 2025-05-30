package com.bank.ivr.auth.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

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
        this.brandConfigurations = new HashMap<>();
        
        // Loop through all brand configurations and build the map
        for (BrandAuthConfiguration config : configurations) {
            String brandCode = config.getBrandCode();
            BrandAuthConfiguration existingConfig = brandConfigurations.get(brandCode);
            
            if (existingConfig == null) {
                // No existing config for this brand, add it
                brandConfigurations.put(brandCode, config);
            } else {
                // There's already a config for this brand, choose based on priority
                if (config.getPriority() > existingConfig.getPriority()) {
                    logger.warn("Multiple configurations found for brand '{}'. Using config with priority {}",
                              brandCode, config.getPriority());
                    brandConfigurations.put(brandCode, config); // Replace with higher priority
                } else {
                    logger.warn("Multiple configurations found for brand '{}'. Using config with priority {}",
                              brandCode, existingConfig.getPriority());
                    // Keep existing config (higher or equal priority)
                }
            }
        }
        
        // Set default configuration (can be overridden by setting a brand with code "DEFAULT")
        this.defaultConfiguration = findDefaultConfiguration();
        
        logger.info("Initialized BrandAuthConfigurationService with {} brand configurations: {}", 
                   brandConfigurations.size(), brandConfigurations.keySet());
    }
    
    /**
     * Finds the default configuration using traditional loop instead of streams.
     */
    private BrandAuthConfiguration findDefaultConfiguration() {
        // Look for explicit default first
        BrandAuthConfiguration defaultConfig = brandConfigurations.get("DEFAULT");
        if (defaultConfig != null) {
            return defaultConfig;
        }
        
        // Find config with lowest priority as fallback
        BrandAuthConfiguration lowestPriorityConfig = null;
        int lowestPriority = Integer.MAX_VALUE;
        
        for (BrandAuthConfiguration config : brandConfigurations.values()) {
            if (config.getPriority() < lowestPriority) {
                lowestPriorityConfig = config;
                lowestPriority = config.getPriority();
            }
        }
        
        return lowestPriorityConfig;
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