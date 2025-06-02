package com.bank.ivr.auth.config.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

/**
 * Configuration for Royal Bank - trust-level-based authentication strategy.
 * Adapts authentication requirements based on customer trust level.
 */
@Component
public class RoyalBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final List<AuthTokenDefinition> TOKEN_DEFINITIONS = Arrays.asList(
        // SSN Last 4 Digits - for high trust scenarios
        AuthTokenDefinition.builder()
            .name("SSN_LAST_4")
            .description("Last 4 digits of Social Security Number")
            .priority(100)
            .maxAttempts(2)
            .inputFormatRegex("\\d{4}")
            .build(),
            
        // Full SSN - for low trust or high risk scenarios
        AuthTokenDefinition.builder()
            .name("SSN_FULL")
            .description("Complete Social Security Number")
            .priority(90)
            .maxAttempts(2)
            .inputFormatRegex("\\d{3}-?\\d{2}-?\\d{4}")
            .build(),
            
        // Debit Card PIN - alternative authentication method
        AuthTokenDefinition.builder()
            .name("DEBIT_CARD_PIN")
            .description("Debit Card PIN")
            .priority(80)
            .maxAttempts(3)
            .inputFormatRegex("^\\d{4}$")
            .build(),
        
        // Date of Birth - fallback option
        AuthTokenDefinition.builder()
            .name("DATE_OF_BIRTH")
            .description("Date of Birth")
            .priority(70)
            .maxAttempts(2)
            .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
            .build()
    );

    private static final Map<String, Integer> BRAND_SPECIFIC_TOKEN_ATTEMPTS;
    static {
        Map<String, Integer> attempts = new HashMap<>();
        attempts.put("SSN_LAST_4", 2);
        attempts.put("SSN_FULL", 2);
        attempts.put("DEBIT_CARD_PIN", 3);
        attempts.put("DATE_OF_BIRTH", 2);
        BRAND_SPECIFIC_TOKEN_ATTEMPTS = attempts;
    }
    
    private static final Map<String, String> BRAND_MESSAGES;
    static {
        Map<String, String> messages = new HashMap<>();
        messages.put("SSN_LAST_4_PROMPT", "For security purposes, please provide the last 4 digits of your Social Security Number.");
        messages.put("SSN_FULL_PROMPT", "For additional verification, please provide your complete Social Security Number.");
        messages.put("TRUST_LEVEL_HIGH", "Your phone number has been verified with high confidence.");
        messages.put("TRUST_LEVEL_LOW", "Additional verification is required for your phone number.");
        messages.put("PHONE_MATCH_MULTIPLE", "Your phone number is associated with multiple accounts. Additional verification is required.");
        messages.put("PHONE_MATCH_NONE", "Your phone number could not be matched with our records. Additional verification is required.");
        BRAND_MESSAGES = messages;
    }

    // Static rule configurations for performance
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
        "TRUST_BASED_SECURITY_RULE",     // Highest priority - core to Royal Bank strategy
        "ROYAL_BANK_TRUST_LEVEL_RULE",   // Brand-specific trust level logic (updated name)
        "HIGH_VALUE_CUSTOMER_RULE",      // High-value customer handling
        "FULL_AUTHENTICATION_COMPLETION_RULE" // Completion checking
    );
    
    private static final Map<String, Integer> RULE_PRIORITIES;
    static {
        Map<String, Integer> priorities = new HashMap<>();
        priorities.put("TRUST_BASED_SECURITY_RULE", 350);      // Highest priority - trust is everything
        priorities.put("ROYAL_BANK_TRUST_LEVEL_RULE", 300);    // Brand-specific trust logic
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 250);       // High-value customer handling
        priorities.put("FULL_AUTHENTICATION_COMPLETION_RULE", 1000); // Completion checking
        RULE_PRIORITIES = priorities;
    }
    
    private static final List<String> APPLICABLE_POST_VALIDATION_RULES = Arrays.asList(
        "TRUST_LEVEL_POST_VALIDATION_RULE",
        "PHONE_MATCH_VERIFICATION_RULE",
        "ROYAL_BANK_RISK_ASSESSMENT_RULE"
    );
    
    private static final List<String> APPLICABLE_ELIGIBILITY_RULES = Arrays.asList(
        "TRUST_LEVEL_ELIGIBILITY_RULE",
        "PHONE_MATCH_ELIGIBILITY_RULE",
        "SSN_VARIANT_ELIGIBILITY_RULE"
    );
    
    @Override
    public String getBrandCode() {
        return "ROYAL_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return TOKEN_DEFINITIONS;
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 4; // Allow more attempts due to trust-based complexity
    }
    
    @Override
    public Map<String, Integer> getBrandSpecificTokenAttempts() {
        return BRAND_SPECIFIC_TOKEN_ATTEMPTS;
    }
    
    @Override
    public boolean isConcurrentTokenAuthAllowed() {
        return false; // Royal Bank prefers sequential authentication
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        return BRAND_MESSAGES;
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for Royal Bank specific configuration
    }
    
    @Override
    public List<String> getApplicableTokenSelectionRules() {
        return APPLICABLE_TOKEN_SELECTION_RULES;
    }
    
    @Override
    public Map<String, Integer> getRulePriorities() {
        return RULE_PRIORITIES;
    }
    
    @Override
    public List<String> getApplicablePostValidationRules() {
        return APPLICABLE_POST_VALIDATION_RULES;
    }
    
    @Override
    public List<String> getApplicableEligibilityRules() {
        return APPLICABLE_ELIGIBILITY_RULES;
    }
} 