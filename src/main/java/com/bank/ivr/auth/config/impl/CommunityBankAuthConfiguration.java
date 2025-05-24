package com.bank.ivr.auth.config.impl;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Authentication configuration for Community Bank brand.
 * More relaxed security requirements with traditional authentication methods.
 */
@Component
public class CommunityBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public String getBrandCode() {
        return "COMMUNITY_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            // Community bank prioritizes SSN for primary authentication (traditional approach)
            AuthTokenDefinition.builder()
                    .name("SSN")
                    .description("Social Security Number")
                    .priority(100) // Highest priority for community bank
                    .maskingRegex("\\d{3}-\\d{2}-(\\d{4})")
                    .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("DATE_OF_BIRTH")
                    .description("Date of Birth")
                    .priority(95)
                    .maskingRegex("(\\d{2})/(\\d{2})/(\\d{4})")
                    .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("MOTHER_MAIDEN_NAME")
                    .description("Mother's Maiden Name")
                    .priority(90)
                    .maskingRegex("(\\w+)")
                    .inputFormatRegex("^[a-zA-Z\\s'-]{2,50}$")
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("DEBIT_CARD_PIN")
                    .description("Debit Card PIN")
                    .priority(85) // Lower priority for community bank
                    .maskingRegex("\\d{4}")
                    .inputFormatRegex("^\\d{4}$")
                    .maxAttempts(3)
                    .build(),
            
            // Community-specific: Account opening date
            AuthTokenDefinition.builder()
                    .name("ACCOUNT_OPENING_DATE")
                    .description("Account Opening Date")
                    .priority(80)
                    .maskingRegex("(\\d{2})/(\\d{4})")
                    .inputFormatRegex("^\\d{2}/\\d{4}$")
                    .maxAttempts(3)
                    .build()
        );
    }
    
    @Override
    public List<String> getRequiredTokens() {
        // Community bank requires only ONE authentication factor (more relaxed)
        return Arrays.asList("SSN");
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 5; // More lenient overall attempts
    }
    
    @Override
    public Map<String, Integer> getBrandSpecificTokenAttempts() {
        Map<String, Integer> attempts = new HashMap<>();
        attempts.put("SSN", 3);
        attempts.put("DATE_OF_BIRTH", 3);
        attempts.put("MOTHER_MAIDEN_NAME", 3);
        attempts.put("DEBIT_CARD_PIN", 3);
        attempts.put("ACCOUNT_OPENING_DATE", 3);
        return attempts;
    }
    
    @Override
    public boolean isConcurrentTokenAuthAllowed() {
        return false; // Community bank prefers step-by-step authentication
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Hello! Welcome to Community Bank. Let's verify your identity.");
        messages.put("primary_prompt", "Please provide the last 4 digits of your Social Security Number.");
        messages.put("secondary_prompt", "Thank you. Please provide your date of birth.");
        messages.put("success", "Great! You're all set. How can we help you today?");
        messages.put("failure", "We couldn't verify your identity. Please visit your local branch or call us at 1-800-COMMUNITY.");
        return messages;
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority configuration
    }
} 