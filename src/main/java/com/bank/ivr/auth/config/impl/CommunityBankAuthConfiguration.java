package com.bank.ivr.auth.config.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

/**
 * Authentication configuration for Community Bank brand.
 * More relaxed security requirements with traditional authentication methods.
 */
@Component
public class CommunityBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final List<AuthTokenDefinition> TOKEN_DEFINITIONS = Arrays.asList(
        // Community bank prioritizes SSN for primary authentication (traditional approach)
        AuthTokenDefinition.builder()
                .name("SSN")
                .description("Social Security Number")
                .priority(100) // Highest priority for community bank
                .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                .maxAttempts(3)
                .build(),
        
        AuthTokenDefinition.builder()
                .name("DATE_OF_BIRTH")
                .description("Date of Birth")
                .priority(95)
                .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                .maxAttempts(3)
                .build(),
        
        AuthTokenDefinition.builder()
                .name("MOTHER_MAIDEN_NAME")
                .description("Mother's Maiden Name")
                .priority(90)
                .inputFormatRegex("^[a-zA-Z\\s'-]{2,50}$")
                .maxAttempts(3)
                .build(),
        
        AuthTokenDefinition.builder()
                .name("DEBIT_CARD_PIN")
                .description("Debit Card PIN")
                .priority(85) // Lower priority for community bank
                .inputFormatRegex("^\\d{4}$")
                .maxAttempts(3)
                .build(),
        
        // Community-specific: Account opening date
        AuthTokenDefinition.builder()
                .name("ACCOUNT_OPENING_DATE")
                .description("Account Opening Date")
                .priority(80)
                .inputFormatRegex("^\\d{2}/\\d{4}$")
                .maxAttempts(3)
                .build()
    );
    
    private static final Map<String, String> BRAND_MESSAGES;
    static {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Community Bank! We're here to help verify your identity.");
        messages.put("primary_prompt", "Please provide your {token_description} to continue.");
        messages.put("secondary_prompt", "Thank you. Please also provide your {token_description}.");
        messages.put("success", "Authentication successful. Welcome to Community Banking!");
        messages.put("failure", "Authentication failed. Please try again or visit your local Community Bank branch.");
        messages.put("customer_not_found", "We couldn't find your account. Please check your information or visit your local branch.");
        messages.put("session_expired", "Your session has timed out. Please start over to verify your identity.");
        messages.put("system_error", "We're experiencing technical difficulties. Please try again or visit your local branch.");
        messages.put("no_methods", "No verification methods available. Please visit your local Community Bank branch for assistance.");
        BRAND_MESSAGES = messages;
    }
    
    // Static rule configurations for performance
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
        "TRUST_BASED_SECURITY_RULE",     // Security first, but lower priority
        "HIGH_VALUE_CUSTOMER_RULE"       // Simple high-value detection
    );
    
    private static final Map<String, Integer> RULE_PRIORITIES;
    static {
        Map<String, Integer> priorities = new HashMap<>();
        priorities.put("TRUST_BASED_SECURITY_RULE", 200);  // Security important but not overwhelming
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 150);   // Moderate priority for high-value customers
        RULE_PRIORITIES = priorities;
    }
    
    private static final List<String> APPLICABLE_POST_VALIDATION_RULES = Arrays.asList(
        "BASIC_SECURITY_CHECK_RULE"
    );
    
    private static final List<String> APPLICABLE_ELIGIBILITY_RULES = Arrays.asList(
        "BASIC_ELIGIBILITY_RULE",
        "ACCOUNT_STATUS_ELIGIBILITY_RULE"
    );
    
    @Override
    public String getBrandCode() {
        return "COMMUNITY_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return TOKEN_DEFINITIONS;
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 5; // More lenient overall attempts
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
        return false; // Community bank prefers step-by-step authentication
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        return BRAND_MESSAGES;
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority configuration
    }
    
    // NEW RULE CONFIGURATION METHODS
    
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