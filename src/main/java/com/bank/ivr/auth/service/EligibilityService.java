package com.bank.ivr.auth.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.DnisConfiguration;
import com.bank.ivr.auth.rule.EligibilityRule;

/**
 * Service responsible for determining which authentication tokens a customer is eligible for.
 * Uses rule-based evaluation with full brand awareness for flexible business logic.
 */
@Service
public class EligibilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(EligibilityService.class);
    
    private final List<EligibilityRule> eligibilityRules;
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public EligibilityService(List<EligibilityRule> eligibilityRules, BrandAuthConfigurationService brandConfigService) {
        this.eligibilityRules = eligibilityRules;
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Determines which tokens a customer is eligible for based on their profile and brand.
     * Uses rule-based evaluation with brand-specific token filtering for flexible business logic.
     */
    public List<String> determineEligibleTokens(CustomerProfile customerProfile, String brand) {
        List<String> eligibleTokens = new ArrayList<>();
        
        logger.debug("Determining brand-aware eligible tokens for customer: {}, brand: {}", 
                    customerProfile.getCustomerId(), brand);
        
        // Get brand-specific token definitions to filter eligibility
        List<AuthTokenDefinition> brandTokenDefinitions = brandConfigService.getTokenDefinitionsForBrand(brand);
        List<String> brandSupportedTokens = new ArrayList<>();
        
        // Extract token names using traditional loop
        for (AuthTokenDefinition tokenDef : brandTokenDefinitions) {
            brandSupportedTokens.add(tokenDef.getName());
        }
        
        logger.debug("Brand '{}' supports tokens: {}", brand, brandSupportedTokens);
        
        // Evaluate eligibility rules
        for (EligibilityRule rule : eligibilityRules) {
            try {
                // Check if rule applies to this brand
                if (!"DEFAULT".equals(rule.getBrand()) && !brand.equals(rule.getBrand())) {
                    continue;
                }
                
                boolean isEligible = rule.isEligible(customerProfile, brand);
                
                if (isEligible) {
                    String tokenName = rule.getTokenName();
                    if (tokenName != null && brandSupportedTokens.contains(tokenName)) {
                        eligibleTokens.add(tokenName);
                        logger.debug("Customer eligible for {} authentication for brand '{}'", tokenName, brand);
                    } else if (tokenName != null) {
                        logger.debug("Customer eligible for {} authentication but token not supported by brand '{}'", 
                                   tokenName, brand);
                    }
                }
            } catch (Exception e) {
                // Log the error but continue with other rules
                logger.error("Error evaluating eligibility rule {}: {}", rule.getRuleName(), e.getMessage());
            }
        }
        
        // Fallback: if no rules worked, use the original hard-coded logic with brand filtering
        if (eligibleTokens.isEmpty()) {
            eligibleTokens = determineFallbackEligibleTokens(customerProfile, brandSupportedTokens, brand);
        }
        
        logger.debug("Final brand-aware eligible tokens for brand '{}': {}", brand, eligibleTokens);
        return eligibleTokens;
    }
    
    /**
     * Determines which tokens a customer is eligible for based on their profile, brand, and DNIS configuration.
     * Uses rule-based evaluation with brand-specific token filtering and DNIS restrictions for flexible business logic.
     */
    public List<String> determineEligibleTokensWithDnis(CustomerProfile customerProfile, String brand, DnisConfiguration dnisConfig) {
        List<String> eligibleTokens = new ArrayList<>();
        
        logger.debug("Determining DNIS-aware eligible tokens for customer: {}, brand: {}, dnis: {}", 
                    customerProfile.getCustomerId(), brand, dnisConfig.getDnis());
        
        // First get brand-eligible tokens
        List<String> brandEligibleTokens = determineEligibleTokens(customerProfile, brand);
        
        // Filter tokens based on DNIS configuration
        for (String tokenName : brandEligibleTokens) {
            if (isTokenAllowedByDnis(tokenName, dnisConfig)) {
                eligibleTokens.add(tokenName);
                logger.debug("Token '{}' allowed for DNIS '{}'", tokenName, dnisConfig.getDnis());
            } else {
                logger.debug("Token '{}' blocked by DNIS configuration '{}'", tokenName, dnisConfig.getDnis());
            }
        }
        
        logger.debug("Final DNIS-aware eligible tokens for brand '{}', DNIS '{}': {}", 
                    brand, dnisConfig.getDnis(), eligibleTokens);
        return eligibleTokens;
    }
    
    /**
     * Checks if a specific token is allowed by the DNIS configuration.
     */
    private boolean isTokenAllowedByDnis(String tokenName, DnisConfiguration dnisConfig) {
        switch (tokenName.toUpperCase()) {
            case "SSN":
            case "SSN_LAST_4":
            case "SSN_FULL":
                return dnisConfig.isAllowSsnAuthentication();
            case "DEBIT_CARD_PIN":
            case "PIN":
                return dnisConfig.isAllowPinAuthentication();
            case "DATE_OF_BIRTH":
                return dnisConfig.isAllowDateOfBirthAuthentication();
            case "MOTHER_MAIDEN_NAME":
                return dnisConfig.isAllowMotherMaidenNameAuthentication();
            case "ACCOUNT_NUMBER":
                return dnisConfig.isAllowAccountNumberAuthentication();
            default:
                // Allow unknown tokens by default (backward compatibility)
                return true;
        }
    }
    
    /**
     * Fallback logic for token eligibility when rules fail, with brand awareness.
     */
    private List<String> determineFallbackEligibleTokens(CustomerProfile customerProfile, 
                                                         List<String> brandSupportedTokens, 
                                                         String brand) {
        logger.debug("Rule evaluation failed, falling back to hard-coded eligibility logic for brand '{}'", brand);
        
        List<String> eligibleTokens = new ArrayList<>();
        
        // Check SSN eligibility
        if (brandSupportedTokens.contains("SSN") && 
            customerProfile.getSsn() != null && !customerProfile.getSsn().trim().isEmpty() 
            && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("SSN");
            logger.debug("Customer eligible for SSN authentication for brand '{}'", brand);
        }
        
        // Check Debit Card PIN eligibility
        if (brandSupportedTokens.contains("DEBIT_CARD_PIN") && 
            customerProfile.getHashedPin() != null && !customerProfile.getHashedPin().trim().isEmpty() 
            && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("DEBIT_CARD_PIN");
            logger.debug("Customer eligible for DEBIT_CARD_PIN authentication for brand '{}'", brand);
        }
        
        // Check Date of Birth eligibility
        if (brandSupportedTokens.contains("DATE_OF_BIRTH") && 
            customerProfile.getDateOfBirth() != null && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("DATE_OF_BIRTH");
            logger.debug("Customer eligible for DATE_OF_BIRTH authentication for brand '{}'", brand);
        }
        
        // Check Mother's Maiden Name eligibility
        if (brandSupportedTokens.contains("MOTHER_MAIDEN_NAME") && 
            customerProfile.getMotherMaidenName() != null && !customerProfile.getMotherMaidenName().trim().isEmpty() 
            && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("MOTHER_MAIDEN_NAME");
            logger.debug("Customer eligible for MOTHER_MAIDEN_NAME authentication for brand '{}'", brand);
        }
        
        return eligibleTokens;
    }
    

} 