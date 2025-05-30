package com.bank.ivr.auth.config.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

/**
 * Configuration for Royal Bank - trust-level-based authentication strategy.
 * Adapts authentication requirements based on customer trust level.
 */
public class RoyalBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public String getBrandCode() {
        return "ROYAL_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
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
    }
    

    
    @Override
    public int getMaxOverallAttempts() {
        return 4; // Allow more attempts due to trust-based complexity
    }
    
    @Override
    public Map<String, Integer> getBrandSpecificTokenAttempts() {
        Map<String, Integer> attempts = new HashMap<>();
        attempts.put("SSN_LAST_4", 2);
        attempts.put("SSN_FULL", 2);
        attempts.put("DEBIT_CARD_PIN", 3);
        attempts.put("DATE_OF_BIRTH", 2);
        return attempts;
    }
    
    @Override
    public boolean isConcurrentTokenAuthAllowed() {
        return false; // Royal Bank prefers sequential authentication
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("SSN_LAST_4_PROMPT", "For security purposes, please provide the last 4 digits of your Social Security Number.");
        messages.put("SSN_FULL_PROMPT", "For additional verification, please provide your complete Social Security Number.");
        messages.put("TRUST_LEVEL_HIGH", "Your phone number has been verified with high confidence.");
        messages.put("TRUST_LEVEL_LOW", "Additional verification is required for your phone number.");
        messages.put("PHONE_MATCH_MULTIPLE", "Your phone number is associated with multiple accounts. Additional verification is required.");
        messages.put("PHONE_MATCH_NONE", "Your phone number could not be matched with our records. Additional verification is required.");
        return messages;
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for Royal Bank specific configuration
    }
} 