package com.bank.ivr.auth.config.impl;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;
import com.bank.ivr.auth.model.domain.TokenRetryStrategy;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
        messages.put("welcome", "Welcome to Community Bank! We're here to help verify your identity.");
        messages.put("primary_prompt", "Please provide your {token_description} to continue.");
        messages.put("secondary_prompt", "Thank you. Please also provide your {token_description}.");
        messages.put("success", "Authentication successful. Welcome to Community Banking!");
        messages.put("failure", "Authentication failed. Please try again or visit your local Community Bank branch.");
        messages.put("customer_not_found", "We couldn't find your account. Please check your information or visit your local branch.");
        messages.put("session_expired", "Your session has timed out. Please start over to verify your identity.");
        messages.put("system_error", "We're experiencing technical difficulties. Please try again or visit your local branch.");
        messages.put("no_methods", "No verification methods available. Please visit your local Community Bank branch for assistance.");
        return messages;
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority configuration
    }
    
    @Override
    public Map<String, TokenRetryStrategy> getTokenRetryStrategies() {
        Map<String, TokenRetryStrategy> strategies = new HashMap<>();
        
        // Community bank uses immediate retry (more user-friendly)
        strategies.put("SSN", TokenRetryStrategy.builder()
                .tokenName("SSN")
                .retryType(TokenRetryStrategy.RetryType.IMMEDIATE)
                .maxRetries(3)
                .progressiveLockoutEnabled(false) // More lenient
                .build());
                
        strategies.put("DEBIT_CARD_PIN", TokenRetryStrategy.builder()
                .tokenName("DEBIT_CARD_PIN")
                .retryType(TokenRetryStrategy.RetryType.FIXED_DELAY)
                .maxRetries(3)
                .baseDelayMs(2000) // 2 second delay
                .progressiveLockoutEnabled(false)
                .build());
                
        strategies.put("DATE_OF_BIRTH", TokenRetryStrategy.builder()
                .tokenName("DATE_OF_BIRTH")
                .retryType(TokenRetryStrategy.RetryType.IMMEDIATE)
                .maxRetries(3)
                .progressiveLockoutEnabled(false)
                .build());
                
        return strategies;
    }
    
    @Override
    public BrandGlobalRetryPolicy getGlobalRetryPolicy() {
        return BrandGlobalRetryPolicy.builder()
                .brandCode("COMMUNITY_BANK")
                .maxGlobalAttempts(8) // More lenient than default
                .globalLockoutEnabled(true)
                .globalLockoutThreshold(10) // Higher threshold
                .globalLockoutDuration(Duration.ofMinutes(5)) // Shorter lockout
                .escalationPolicy(BrandGlobalRetryPolicy.EscalationPolicy.NONE) // No escalation
                .crossTokenDelayEnabled(false) // No cross-token delays
                .suspiciousActivityThreshold(12) // Higher threshold
                .retryWindowResetDuration(Duration.ofMinutes(30)) // Faster reset
                .enableRetryAnalytics(true)
                .build();
    }
} 