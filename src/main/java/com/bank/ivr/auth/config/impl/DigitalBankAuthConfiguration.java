package com.bank.ivr.auth.config.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

/**
 * Configuration for Digital Bank - modern biometric-first authentication.
 * Demonstrates brand-configured rules approach.
 */
@Component
public class DigitalBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final List<AuthTokenDefinition> TOKEN_DEFINITIONS = Arrays.asList(
        // Modern biometric - highest priority
        AuthTokenDefinition.builder()
            .name("FACE_ID")
            .description("Face ID Biometric Authentication")
            .priority(200)
            .maxAttempts(1)  // No retries for biometrics
            .inputFormatRegex("^[A-F0-9]{64}$")  // 64-char hex hash
            .build(),
            
        // Mobile PIN - second choice
        AuthTokenDefinition.builder()
            .name("MOBILE_PIN")
            .description("6-Digit Mobile Banking PIN")
            .priority(150)
            .maxAttempts(3)
            .inputFormatRegex("^\\d{6}$")
            .build(),
            
        // Traditional fallback
        AuthTokenDefinition.builder()
            .name("SSN_LAST_4")
            .description("Last 4 digits of Social Security Number")
            .priority(100)
            .maxAttempts(2)
            .inputFormatRegex("^\\d{4}$")
            .build()
    );
    
    private static final Map<String, String> BRAND_MESSAGES = new HashMap<String, String>() {{
        put("welcome", "Welcome to Digital Bank. Please authenticate using your preferred method.");
        put("failure", "Authentication failed. Please visit our mobile app or call customer service.");
        put("no_methods", "No authentication methods available. Please update your profile in our mobile app.");
        put("customer_not_found", "Customer profile not found. Please verify your information.");
        put("session_expired", "Your session has expired. Please call again to restart authentication.");
        put("success", "Authentication successful. Welcome to Digital Bank!");
    }};
    
    // Static rule configurations for performance
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
        "TRUST_BASED_SECURITY_RULE",        // Priority: 300 (security first)
        "HIGH_VALUE_CUSTOMER_RULE",         // Priority: 250 (shared rule)
        "DIGITAL_BANK_AGE_RULE",           // Priority: 200 (brand-specific)
        "DIGITAL_BANK_BIOMETRIC_PREFERENCE" // Priority: 180 (brand-specific)
    );
    
    private static final Map<String, Integer> RULE_PRIORITIES;
    static {
        Map<String, Integer> priorities = new HashMap<>();
        priorities.put("TRUST_BASED_SECURITY_RULE", 300);
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 250);
        priorities.put("DIGITAL_BANK_AGE_RULE", 200);
        priorities.put("DIGITAL_BANK_BIOMETRIC_PREFERENCE", 180);
        RULE_PRIORITIES = priorities;
    }
    
    private static final List<String> APPLICABLE_POST_VALIDATION_RULES = Arrays.asList(
        "HIGH_VALUE_SECURITY_RULE",
        "DIGITAL_BANK_COMPLIANCE_RULE"
    );
    
    private static final List<String> APPLICABLE_ELIGIBILITY_RULES = Arrays.asList(
        "BIOMETRIC_ELIGIBILITY_RULE",
        "MOBILE_PIN_ELIGIBILITY_RULE"
    );
    
    @Override
    public String getBrandCode() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return TOKEN_DEFINITIONS;
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 3;  // Strict security for digital bank
    }
    
    @Override
    public Map<String, Integer> getBrandSpecificTokenAttempts() {
        return new HashMap<>(); // Use token definition defaults
    }
    
    @Override
    public boolean isConcurrentTokenAuthAllowed() {
        return false;  // Sequential authentication for security
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        return BRAND_MESSAGES;
    }
    
    @Override
    public int getPriority() {
        return 100;
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