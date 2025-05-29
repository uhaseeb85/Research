package com.bank.ivr.auth.config.impl;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;
import com.bank.ivr.auth.model.domain.TokenRetryStrategy;

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
                    .inputFormatRegex("^\\d{4}$")
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("SSN")
                    .description("Social Security Number")
                    .priority(95)
                    .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                    .maxAttempts(2) // Stricter for premium
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("DATE_OF_BIRTH")
                    .description("Date of Birth")
                    .priority(90)
                    .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("MOTHER_MAIDEN_NAME")
                    .description("Mother's Maiden Name")
                    .priority(85)
                    .inputFormatRegex("^[a-zA-Z\\s'-]{2,50}$")
                    .maxAttempts(2)
                    .build(),
            
            // Premium feature: Voice biometric
            AuthTokenDefinition.builder()
                    .name("VOICE_BIOMETRIC")
                    .description("Voice Authentication")
                    .priority(80)
                    .inputFormatRegex("^VOICE_MATCH$")
                    .maxAttempts(2)
                    .build()
        );
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
        messages.put("primary_prompt", "Please provide your {token_description} for secure access.");
        messages.put("secondary_prompt", "Thank you. For additional security, please provide your {token_description}.");
        messages.put("success", "Authentication successful. Welcome to Premium Banking services.");
        messages.put("failure", "Authentication failed. Please contact Premium Support at 1-800-PREMIUM.");
        messages.put("customer_not_found", "Customer account not found. Please verify your information or contact Premium Support.");
        messages.put("session_expired", "Your secure session has expired. Please start the authentication process again.");
        messages.put("system_error", "A system error occurred. Please try again or contact Premium Support at 1-800-PREMIUM.");
        messages.put("no_methods", "No authentication methods available. Please contact Premium Support at 1-800-PREMIUM.");
        return messages;
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority configuration
    }
    
    @Override
    public Map<String, TokenRetryStrategy> getTokenRetryStrategies() {
        Map<String, TokenRetryStrategy> strategies = new HashMap<>();
        
        // Premium bank uses strict exponential backoff for security
        strategies.put("DEBIT_CARD_PIN", TokenRetryStrategy.builder()
                .tokenName("DEBIT_CARD_PIN")
                .retryType(TokenRetryStrategy.RetryType.EXPONENTIAL_BACKOFF)
                .maxRetries(2) // Fewer retries
                .baseDelayMs(5000) // 5 second base delay
                .maxDelayMs(60000) // 1 minute max
                .multiplier(2.0)
                .progressiveLockoutEnabled(true)
                .lockoutDurationAfterExhaustion(Duration.ofMinutes(15))
                .build());
                
        strategies.put("SSN", TokenRetryStrategy.builder()
                .tokenName("SSN")
                .retryType(TokenRetryStrategy.RetryType.EXPONENTIAL_BACKOFF)
                .maxRetries(2)
                .baseDelayMs(3000)
                .maxDelayMs(30000)
                .progressiveLockoutEnabled(true)
                .lockoutDurationAfterExhaustion(Duration.ofMinutes(10))
                .build());
                
        strategies.put("VOICE_BIOMETRIC", TokenRetryStrategy.builder()
                .tokenName("VOICE_BIOMETRIC")
                .retryType(TokenRetryStrategy.RetryType.LINEAR_BACKOFF)
                .maxRetries(2)
                .baseDelayMs(10000) // Longer delay for biometric
                .progressiveLockoutEnabled(true)
                .build());
                
        return strategies;
    }
    
    @Override
    public BrandGlobalRetryPolicy getGlobalRetryPolicy() {
        return BrandGlobalRetryPolicy.builder()
                .brandCode("PREMIUM_BANK")
                .maxGlobalAttempts(5) // Strict limit
                .globalLockoutEnabled(true)
                .globalLockoutThreshold(4) // Low threshold
                .globalLockoutDuration(Duration.ofMinutes(20)) // Longer lockout
                .escalationPolicy(BrandGlobalRetryPolicy.EscalationPolicy.PROGRESSIVE_DELAY)
                .escalationThreshold(3)
                .crossTokenDelayEnabled(true) // Enable cross-token delays
                .crossTokenDelayMultiplier(2.0)
                .suspiciousActivityThreshold(4) // Low threshold for premium security
                .suspiciousActivityLockoutDuration(Duration.ofMinutes(30))
                .retryWindowResetDuration(Duration.ofHours(2)) // Longer reset window
                .enableRetryAnalytics(true)
                .build();
    }
} 