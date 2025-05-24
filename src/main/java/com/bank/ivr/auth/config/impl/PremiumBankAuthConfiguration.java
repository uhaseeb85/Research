package com.bank.ivr.auth.config.impl;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Authentication configuration for Premium Bank brand.
 * High security requirements with multiple authentication factors.
 */
@Component
public class PremiumBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public String getBrandCode() {
        return "PREMIUM_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            // Premium bank prioritizes PIN over SSN for primary authentication
            AuthTokenDefinition.builder()
                    .name("DEBIT_CARD_PIN")
                    .description("Debit Card PIN")
                    .priority(100) // Highest priority
                    .maskingRegex("\\d{4}")
                    .inputFormatRegex("^\\d{4}$")
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("SSN")
                    .description("Social Security Number")
                    .priority(95)
                    .maskingRegex("\\d{3}-\\d{2}-(\\d{4})")
                    .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                    .maxAttempts(2) // Stricter for premium
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("DATE_OF_BIRTH")
                    .description("Date of Birth")
                    .priority(90)
                    .maskingRegex("(\\d{2})/(\\d{2})/(\\d{4})")
                    .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("MOTHER_MAIDEN_NAME")
                    .description("Mother's Maiden Name")
                    .priority(85)
                    .maskingRegex("(\\w+)")
                    .inputFormatRegex("^[a-zA-Z\\s'-]{2,50}$")
                    .maxAttempts(2)
                    .build(),
            
            // Premium feature: Voice biometric
            AuthTokenDefinition.builder()
                    .name("VOICE_BIOMETRIC")
                    .description("Voice Authentication")
                    .priority(80)
                    .maskingRegex("(VOICE_MATCH)")
                    .inputFormatRegex("^VOICE_MATCH$")
                    .maxAttempts(2)
                    .build()
        );
    }
    
    @Override
    public List<String> getRequiredTokens() {
        // Premium bank requires TWO authentication factors
        return Arrays.asList("DEBIT_CARD_PIN", "DATE_OF_BIRTH");
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 3; // Stricter overall attempts for premium security
    }
    
    @Override
    public Map<String, Integer> getBrandSpecificTokenAttempts() {
        Map<String, Integer> attempts = new HashMap<>();
        attempts.put("SSN", 2); // Override to be more restrictive
        attempts.put("DEBIT_CARD_PIN", 3);
        attempts.put("DATE_OF_BIRTH", 3);
        attempts.put("MOTHER_MAIDEN_NAME", 2);
        attempts.put("VOICE_BIOMETRIC", 2);
        return attempts;
    }
    
    @Override
    public boolean isConcurrentTokenAuthAllowed() {
        return true; // Premium customers can provide multiple tokens at once
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Premium Bank. For your security, we require enhanced authentication.");
        messages.put("primary_prompt", "Please provide your 4-digit PIN.");
        messages.put("secondary_prompt", "Please also provide your date of birth for additional verification.");
        messages.put("success", "Authentication successful. Welcome to Premium Banking services.");
        messages.put("failure", "Authentication failed. Please contact Premium Support at 1-800-PREMIUM.");
        return messages;
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority configuration
    }
} 