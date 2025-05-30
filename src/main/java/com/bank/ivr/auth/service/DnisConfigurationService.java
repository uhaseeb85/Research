package com.bank.ivr.auth.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.DnisConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for managing DNIS (Dialed Number Identification Service) configurations.
 * Loads DNIS-specific authentication rules from JSON configuration files.
 */
@Service
public class DnisConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DnisConfigurationService.class);
    
    private static final Map<String, Function<DnisConfiguration, Boolean>> TOKEN_VALIDATORS;
    static {
        Map<String, Function<DnisConfiguration, Boolean>> validators = new HashMap<>();
        validators.put("SSN", DnisConfiguration::isAllowSsnAuthentication);
        validators.put("DEBIT_CARD_PIN", DnisConfiguration::isAllowPinAuthentication);
        validators.put("PIN", DnisConfiguration::isAllowPinAuthentication);
        validators.put("DATE_OF_BIRTH", DnisConfiguration::isAllowDateOfBirthAuthentication);
        validators.put("MOTHER_MAIDEN_NAME", DnisConfiguration::isAllowMotherMaidenNameAuthentication);
        validators.put("ACCOUNT_NUMBER", DnisConfiguration::isAllowAccountNumberAuthentication);
        TOKEN_VALIDATORS = validators;
    }
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final Map<String, DnisConfiguration> dnisConfigurations = new HashMap<>();
    
    @Value("${app.dnis.config-file:classpath:dnis-configurations.json}")
    private String dnisConfigFile;
    
    public DnisConfigurationService(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void loadDnisConfigurations() {
        try {
            Resource resource = resourceLoader.getResource(dnisConfigFile);
            if (resource.exists()) {
                loadConfigurationsFromResource(resource);
            } else {
                logger.warn("DNIS configuration file not found: {}. Using default configurations.", dnisConfigFile);
                loadDefaultConfigurations();
            }
        } catch (Exception e) {
            logger.error("Failed to load DNIS configurations from: {}. Using default configurations.", dnisConfigFile, e);
            loadDefaultConfigurations();
        }
    }
    
    private void loadConfigurationsFromResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            List<DnisConfiguration> configurations = objectMapper.readValue(
                inputStream, 
                new TypeReference<List<DnisConfiguration>>() {}
            );
            
            for (DnisConfiguration config : configurations) {
                dnisConfigurations.put(config.getDnis(), config);
                logger.info("Loaded DNIS configuration for: {} - {}", config.getDnis(), config.getDescription());
            }
            
            logger.info("Successfully loaded {} DNIS configurations", dnisConfigurations.size());
        }
    }
    
    private void loadDefaultConfigurations() {
        // Default configuration for standard 1-800 numbers
        DnisConfiguration defaultConfig = new DnisConfiguration(
            "DEFAULT",
            "Default DNIS configuration for all unspecified numbers",
            true,  // allowSsnAuthentication
            true,  // allowPinAuthentication
            true,  // allowDateOfBirthAuthentication
            true,  // allowMotherMaidenNameAuthentication
            true,  // allowAccountNumberAuthentication
            false, // requireMultiFactorAuth
            false, // allowTrustLevelBypass
            true,  // enablePhoneMatchValidation
            true,  // allowAlternativeTokens
            false, // enableStrictValidation
            true,  // allowRetryOnFailure
            true,  // enableAuditLogging
            3,     // maxAuthenticationAttempts
            15     // sessionTimeoutMinutes
        );
        
        // High security configuration for premium services
        DnisConfiguration premiumConfig = new DnisConfiguration(
            "18001234567",
            "Premium banking services - high security",
            true,  // allowSsnAuthentication
            true,  // allowPinAuthentication
            true,  // allowDateOfBirthAuthentication
            false, // allowMotherMaidenNameAuthentication (disabled for security)
            true,  // allowAccountNumberAuthentication
            true,  // requireMultiFactorAuth
            false, // allowTrustLevelBypass
            true,  // enablePhoneMatchValidation
            false, // allowAlternativeTokens (strict mode)
            true,  // enableStrictValidation
            false, // allowRetryOnFailure (strict mode)
            true,  // enableAuditLogging
            2,     // maxAuthenticationAttempts (reduced for security)
            10     // sessionTimeoutMinutes (reduced for security)
        );
        
        // Basic configuration for general customer service
        DnisConfiguration basicConfig = new DnisConfiguration(
            "18009876543",
            "General customer service - standard security",
            true,  // allowSsnAuthentication
            true,  // allowPinAuthentication
            true,  // allowDateOfBirthAuthentication
            true,  // allowMotherMaidenNameAuthentication
            true,  // allowAccountNumberAuthentication
            false, // requireMultiFactorAuth
            true,  // allowTrustLevelBypass (for customer service)
            false, // enablePhoneMatchValidation (relaxed for CS)
            true,  // allowAlternativeTokens
            false, // enableStrictValidation
            true,  // allowRetryOnFailure
            true,  // enableAuditLogging
            5,     // maxAuthenticationAttempts (higher for CS)
            20     // sessionTimeoutMinutes (longer for CS)
        );
        
        dnisConfigurations.put("DEFAULT", defaultConfig);
        dnisConfigurations.put("18001234567", premiumConfig);
        dnisConfigurations.put("18009876543", basicConfig);
        
        logger.info("Loaded {} default DNIS configurations", dnisConfigurations.size());
    }
    
    /**
     * Get DNIS configuration for a specific DNIS number.
     * Returns default configuration if specific DNIS is not found.
     */
    public DnisConfiguration getDnisConfiguration(String dnis) {
        if (dnis == null || dnis.trim().isEmpty()) {
            return dnisConfigurations.get("DEFAULT");
        }
        
        // Clean the DNIS (remove any formatting)
        String cleanDnis = dnis.replaceAll("[^0-9]", "");
        
        return dnisConfigurations.getOrDefault(cleanDnis, dnisConfigurations.get("DEFAULT"));
    }
    
    /**
     * Check if a specific authentication token is allowed for the given DNIS.
     */
    public boolean isTokenAllowedForDnis(String dnis, String tokenName) {
        DnisConfiguration config = getDnisConfiguration(dnis);
        
        Function<DnisConfiguration, Boolean> validator = TOKEN_VALIDATORS.get(tokenName.toUpperCase());
        if (validator != null) {
            return validator.apply(config);
        }
        
        return true; // Allow unknown tokens by default
    }
    
    /**
     * Get all available DNIS configurations.
     */
    public Map<String, DnisConfiguration> getAllDnisConfigurations() {
        return new HashMap<>(dnisConfigurations);
    }
    
    /**
     * Check if multi-factor authentication is required for the given DNIS.
     */
    public boolean isMultiFactorAuthRequired(String dnis) {
        return getDnisConfiguration(dnis).isRequireMultiFactorAuth();
    }
    
    /**
     * Get maximum authentication attempts allowed for the given DNIS.
     */
    public int getMaxAuthenticationAttempts(String dnis) {
        return getDnisConfiguration(dnis).getMaxAuthenticationAttempts();
    }
    
    /**
     * Get session timeout in minutes for the given DNIS.
     */
    public int getSessionTimeoutMinutes(String dnis) {
        return getDnisConfiguration(dnis).getSessionTimeoutMinutes();
    }
    
    /**
     * Check if alternative tokens are allowed for the given DNIS.
     */
    public boolean areAlternativeTokensAllowed(String dnis) {
        return getDnisConfiguration(dnis).isAllowAlternativeTokens();
    }
    
    /**
     * Check if trust level bypass is allowed for the given DNIS.
     */
    public boolean isTrustLevelBypassAllowed(String dnis) {
        return getDnisConfiguration(dnis).isAllowTrustLevelBypass();
    }
} 