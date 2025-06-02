package com.bank.ivr.auth.config.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

/**
 * Authentication configuration for Royal Bank brand.
 * Trust-level-based authentication with SSN variants.
 */
@Component
public class RoyalBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final List<AuthTokenDefinition> TOKEN_DEFINITIONS = Arrays.asList(
        // Royal Bank uses different SSN approaches based on trust level
        AuthTokenDefinition.builder()
                .name("SSN_FULL")
                .description("Full Social Security Number")
                .priority(100)
                .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                .maxAttempts(2)
                .build(),
        
        AuthTokenDefinition.builder()
                .name("SSN_LAST_4")
                .description("Last 4 digits of Social Security Number")
                .priority(90)
                .inputFormatRegex("^\\d{4}$")
                .maxAttempts(3)
                .build(),
        
        AuthTokenDefinition.builder()
                .name("DEBIT_CARD_PIN")
                .description("Debit Card PIN")
                .priority(80)
                .inputFormatRegex("^\\d{4}$")
                .maxAttempts(3)
                .build(),
        
        AuthTokenDefinition.builder()
                .name("DATE_OF_BIRTH")
                .description("Date of Birth")
                .priority(70)
                .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                .maxAttempts(3)
                .build()
    );

    private static final Map<String, String> BRAND_MESSAGES;
    static {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Royal Bank. We use advanced security to protect your account.");
        messages.put("primary_prompt", "For your security, please provide your {token_description}.");
        messages.put("secondary_prompt", "Thank you. Please also provide your {token_description} for verification.");
        messages.put("success", "Authentication successful. Welcome to Royal Banking!");
        messages.put("failure", "Authentication failed. Please contact Royal Bank Customer Service at 1-800-ROYAL.");
        messages.put("customer_not_found", "Account not found. Please verify your information or contact Royal Bank Customer Service.");
        messages.put("session_expired", "Your secure session has expired. Please restart the authentication process.");
        messages.put("system_error", "System error encountered. Please try again or contact Royal Bank Customer Service.");
        messages.put("no_methods", "No authentication methods available. Please contact Royal Bank Customer Service at 1-800-ROYAL.");
        BRAND_MESSAGES = messages;
    }
    
    // Static rule configurations for performance optimization
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
        "TRUST_BASED_SECURITY_RULE",      // Priority: 350 (highest - trust is everything)
        "ROYAL_BANK_TRUST_LEVEL_RULE",    // Priority: 300 (brand-specific trust logic)
        "HIGH_VALUE_CUSTOMER_RULE"        // Priority: 250 (high-value customer handling)
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
        // Dynamically extract max attempts from token definitions - single source of truth
        Map<String, Integer> attempts = new HashMap<>();
        for (AuthTokenDefinition token : TOKEN_DEFINITIONS) {
            attempts.put(token.getName(), token.getMaxAttempts());
        }
        return attempts;
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