# Bank Onboarding Guide - IVR Authentication System

This comprehensive guide walks you through the process of adding a new bank (brand) to the IVR Authentication System, including implementing custom authentication tokens and validation rules.

## 🆕 Latest Updates (v1.3)

This guide reflects the most recent system improvements:
- **🔥 NEW: Trust-Based Authentication**: Advanced conditional authentication based on trust levels and phone matching
- **Royal Bank Implementation**: Complete example of trust-based authentication with hundreds of conditional scenarios
- **Enhanced Request Model**: `AuthenticationRequest` now includes `TrustLevelInfo` for advanced authentication flows
- **Conditional Rules**: New `ConditionalAuthenticationRule` interface for complex authentication logic
- **Codebase Cleanup**: Removed deprecated methods and simplified APIs
- **Enhanced Error Handling**: Streamlined controller logging and error responses
- **Improved Code Quality**: Fixed compilation issues and updated test expectations

📋 **For detailed information about all improvements, see [CODEBASE_CLEANUP_SUMMARY.md](CODEBASE_CLEANUP_SUMMARY.md)**
📋 **For trust-based authentication details, see [ROYAL_BANK_TRUST_AUTHENTICATION.md](ROYAL_BANK_TRUST_AUTHENTICATION.md)**

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Step 1: Define Your Bank's Authentication Strategy](#step-1-define-your-banks-authentication-strategy)
4. [Step 2: Implement Custom Token Validators](#step-2-implement-custom-token-validators)
5. [Step 3: Create Brand Configuration](#step-3-create-brand-configuration)
6. [Step 4: Database/Data Model Extensions](#step-4-databasedata-model-extensions)
7. [Step 5: Testing Your Implementation](#step-5-testing-your-implementation)
8. [Step 6: Configuration and Deployment](#step-6-configuration-and-deployment)
9. [Smart Token Re-asking Logic](#smart-token-re-asking-logic)
10. [Trust-Based Authentication (Advanced)](#trust-based-authentication-advanced)
11. [Troubleshooting](#troubleshooting)
12. [Best Practices](#best-practices)
13. [Migration Guide](#migration-guide)

## Overview

The IVR Authentication System supports multiple banks (brands) with their own authentication requirements. Each bank can have:

- **Custom Token Types**: Unique authentication methods (PIN, SSN, biometrics, etc.)
- **Brand-Specific Validation Logic**: Different rules for validating the same token type
- **Custom Retry Policies**: Brand-specific attempt limits and lockout rules
- **Tailored User Experience**: Custom messages and prompts
- **🆕 Trust-Based Authentication**: Advanced conditional logic based on trust levels and phone matching

### Key Architecture Components

- **TokenValidator**: Interface for implementing token validation logic
- **BrandAuthConfiguration**: Interface for defining brand-specific authentication rules
- **TokenValidationService**: Central service that manages all validators (brand-aware)
- **CustomerProfile**: Data model containing customer authentication information

### Current System Status

✅ **Fully Operational Components**:
- Brand-aware token validation
- Simplified encryption utilities (`EncryptionUtil.hash()` and `EncryptionUtil.verify()`)
- Streamlined controller logging
- Clean import statements
- Comprehensive test coverage

## Prerequisites

Before starting, ensure you have:

1. **Development Environment**: Java 17+, Spring Boot 3.1+, Maven
2. **Database Access**: Ability to modify customer profile schema if needed
3. **Brand Information**: 
   - Brand code (unique identifier)
   - Required authentication tokens
   - Security policies and rules
   - Custom messages and prompts
4. **Test Customer Data**: Sample customer profiles for testing

## Step 1: Define Your Bank's Authentication Strategy

### 1.1 Choose Your Brand Code

Select a unique brand code that will identify your bank throughout the system.

**Examples:**
- `METRO_CREDIT_UNION`
- `FIRST_NATIONAL_BANK`
- `TECH_BANK`

### 1.2 Define Authentication Requirements

Document your bank's authentication strategy:

```yaml
Brand: TECH_BANK
Required Tokens: 
  - Primary: MOBILE_PIN (4-6 digits)
  - Secondary: BIOMETRIC_ID or ACCOUNT_NUMBER
Optional Tokens:
  - SECURITY_QUESTION
  - EMAIL_VERIFICATION
Security Policies:
  - Max Overall Attempts: 5
  - Individual Token Attempts: 3
  - Lockout Duration: 15 minutes
  - Two-Factor Required: Yes
```

### 1.3 Plan Token Priority

Determine the order in which tokens should be requested:

1. **Primary Token** (highest priority): Most secure/preferred method
2. **Secondary Tokens**: Fallback options
3. **Backup Tokens**: Emergency authentication methods

## Step 2: Implement Custom Token Validators

### 2.1 Create Basic Token Validator

For each custom token type, create a validator class implementing `TokenValidator`:

```java
// File: src/main/java/com/bank/ivr/auth/validator/impl/TechBankMobilePinValidator.java
package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.util.EncryptionUtil;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tech Bank specific Mobile PIN validator.
 * Supports 4-6 digit PINs with brand-specific security rules.
 */
@Component
public class TechBankMobilePinValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(TechBankMobilePinValidator.class);
    
    @Override
    public String getTokenName() {
        return "MOBILE_PIN";
    }
    
    @Override
    public String getBrand() {
        return "TECH_BANK";
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        if (customerProfile.getMobilePin() == null || providedTokenValue == null) {
            logger.debug("Mobile PIN validation failed: null values");
            return false;
        }
        
        String normalizedProvided = normalizeTokenValue(providedTokenValue);
        
        // Validate PIN format (4-6 digits for Tech Bank)
        if (!isValidTechBankPinFormat(normalizedProvided)) {
            logger.debug("Mobile PIN validation failed: invalid format for customer {}", customerIdentifierValue);
            return false;
        }
        
        try {
            // Use the simplified encryption utility
            boolean isValid = EncryptionUtil.verify(normalizedProvided, customerProfile.getMobilePin());
            
            if (isValid) {
                logger.debug("Mobile PIN validation successful for customer {}", customerIdentifierValue);
            } else {
                logger.debug("Mobile PIN validation failed for customer {}", customerIdentifierValue);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating Mobile PIN for customer {}: {}", customerIdentifierValue, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Remove all non-digit characters
        return providedTokenValue.replaceAll("[^0-9]", "");
    }
    
    @Override
    public int getPriority() {
        return 150; // High priority for this brand
    }
    
    private boolean isValidTechBankPinFormat(String pin) {
        // Tech Bank allows 4-6 digit PINs
        return pin != null && pin.matches("^\\d{4,6}$");
    }
}
```

### 2.2 Important Notes on Encryption

⚠️ **Updated Encryption Methods**: The system now uses simplified encryption utilities:

- **Use**: `EncryptionUtil.hash(plainText)` for hashing
- **Use**: `EncryptionUtil.verify(plainText, hashedText)` for verification
- **Deprecated**: `hashPin()` and `verifyPin()` methods have been removed

### 2.3 Create Additional Validators

For each token type your bank uses, create similar validator classes. Here's an example for a biometric ID validator:

```java
// File: src/main/java/com/bank/ivr/auth/validator/impl/TechBankBiometricValidator.java
package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TechBankBiometricValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(TechBankBiometricValidator.class);
    
    @Override
    public String getTokenName() {
        return "BIOMETRIC_ID";
    }
    
    @Override
    public String getBrand() {
        return "TECH_BANK";
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        // Implement biometric validation logic
        // This could involve calling external biometric services
        
        if (customerProfile.getBiometricId() == null || providedTokenValue == null) {
            logger.debug("Biometric validation failed: null values");
            return false;
        }
        
        // Example: Simple string comparison (in reality, this would be more complex)
        boolean isValid = providedTokenValue.equals(customerProfile.getBiometricId());
        
        logger.debug("Biometric validation {} for customer {}", 
                    isValid ? "successful" : "failed", customerIdentifierValue);
        
        return isValid;
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Biometric IDs might need specific normalization
        return providedTokenValue.trim().toUpperCase();
    }
    
    @Override
    public int getPriority() {
        return 120; // Medium-high priority
    }
}
```

## Step 3: Create Brand Configuration

### 3.1 Implement BrandAuthConfiguration

Create a configuration class that defines your bank's authentication rules:

```java
// File: src/main/java/com/bank/ivr/auth/config/impl/TechBankAuthConfiguration.java
package com.bank.ivr.auth.config.impl;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.BrandFailurePolicy;
import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;
import com.bank.ivr.auth.model.domain.TokenRetryStrategy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication configuration for Tech Bank.
 * Focuses on mobile-first authentication with biometric fallback.
 */
@Component
public class TechBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public String getBrandCode() {
        return "TECH_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            // Primary: Mobile PIN
            AuthTokenDefinition.builder()
                    .name("MOBILE_PIN")
                    .description("Mobile Banking PIN")
                    .priority(150) // Highest priority
                    .maskingRegex("\\d{4,6}")
                    .inputFormatRegex("^\\d{4,6}$")
                    .maxAttempts(3)
                    .build(),
            
            // Secondary: Biometric ID
            AuthTokenDefinition.builder()
                    .name("BIOMETRIC_ID")
                    .description("Biometric Authentication")
                    .priority(120)
                    .maskingRegex("(BIOMETRIC_MATCH)")
                    .inputFormatRegex("^[A-Z0-9_]+$")
                    .maxAttempts(2)
                    .build(),
            
            // Fallback: Account Number
            AuthTokenDefinition.builder()
                    .name("ACCOUNT_NUMBER")
                    .description("Account Number")
                    .priority(100)
                    .maskingRegex("\\d{8,12}")
                    .inputFormatRegex("^\\d{8,12}$")
                    .maxAttempts(3)
                    .build()
        );
    }
    
    @Override
    public List<String> getRequiredTokens() {
        // Tech Bank requires mobile PIN and one additional factor
        return Arrays.asList("MOBILE_PIN", "BIOMETRIC_ID");
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 4; // Moderate security
    }
    
    @Override
    public Map<String, Integer> getBrandSpecificTokenAttempts() {
        Map<String, Integer> attempts = new HashMap<>();
        attempts.put("MOBILE_PIN", 3);
        attempts.put("BIOMETRIC_ID", 2);
        attempts.put("ACCOUNT_NUMBER", 3);
        return attempts;
    }
    
    @Override
    public boolean isConcurrentTokenAuthAllowed() {
        return true; // Allow multiple tokens at once for better UX
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Tech Bank! Let's verify your identity using our secure mobile authentication.");
        messages.put("primary_prompt", "Please provide your {token_description} to continue.");
        messages.put("secondary_prompt", "For additional security, please provide your {token_description}.");
        messages.put("success", "Authentication successful. Welcome to Tech Bank Digital Services!");
        messages.put("failure", "Authentication failed. Please try again or contact Tech Bank Support at 1-800-TECH-BANK.");
        messages.put("customer_not_found", "Account not found. Please verify your information or contact Tech Bank Support.");
        messages.put("session_expired", "Your session has expired. Please start the authentication process again.");
        messages.put("system_error", "Technical difficulties encountered. Please try again or contact Tech Bank Support.");
        messages.put("no_methods", "No authentication methods available. Please contact Tech Bank Support at 1-800-TECH-BANK.");
        return messages;
    }
    
    @Override
    public int getPriority() {
        return 75; // Medium priority configuration
    }
    
    @Override
    public Map<String, TokenRetryStrategy> getTokenRetryStrategies() {
        Map<String, TokenRetryStrategy> strategies = new HashMap<>();
        
        // Mobile PIN: Progressive delay for security
        strategies.put("MOBILE_PIN", TokenRetryStrategy.builder()
                .tokenName("MOBILE_PIN")
                .retryType(TokenRetryStrategy.RetryType.LINEAR_BACKOFF)
                .maxRetries(3)
                .baseDelayMs(3000) // 3 second delay
                .progressiveLockoutEnabled(true)
                .lockoutDurationAfterExhaustion(Duration.ofMinutes(10))
                .build());
                
        // Biometric: Immediate retry (biometric failures are usually immediate)
        strategies.put("BIOMETRIC_ID", TokenRetryStrategy.builder()
                .tokenName("BIOMETRIC_ID")
                .retryType(TokenRetryStrategy.RetryType.IMMEDIATE)
                .maxRetries(2)
                .progressiveLockoutEnabled(false)
                .build());
                
        return strategies;
    }
    
    @Override
    public BrandGlobalRetryPolicy getGlobalRetryPolicy() {
        return BrandGlobalRetryPolicy.builder()
                .brandCode("TECH_BANK")
                .maxGlobalAttempts(6)
                .globalLockoutEnabled(true)
                .globalLockoutThreshold(5)
                .globalLockoutDuration(Duration.ofMinutes(15))
                .escalationPolicy(BrandGlobalRetryPolicy.EscalationPolicy.PROGRESSIVE_DELAY)
                .escalationThreshold(3)
                .crossTokenDelayEnabled(true)
                .crossTokenDelayMultiplier(1.5)
                .suspiciousActivityThreshold(6)
                .suspiciousActivityLockoutDuration(Duration.ofMinutes(20))
                .retryWindowResetDuration(Duration.ofHours(1))
                .enableRetryAnalytics(true)
                .build();
    }
    
    @Override
    public BrandFailurePolicy getBrandFailurePolicy() {
        Map<String, List<String>> tokenAlternatives = new HashMap<>();
        tokenAlternatives.put("MOBILE_PIN", Arrays.asList("BIOMETRIC_ID", "ACCOUNT_NUMBER"));
        tokenAlternatives.put("BIOMETRIC_ID", Arrays.asList("ACCOUNT_NUMBER"));
        
        Map<String, List<String>> tokenGroups = new HashMap<>();
        tokenGroups.put("PRIMARY_AUTH", Arrays.asList("MOBILE_PIN"));
        tokenGroups.put("SECONDARY_AUTH", Arrays.asList("BIOMETRIC_ID", "ACCOUNT_NUMBER"));
        
        return BrandFailurePolicy.builder()
                .brandCode("TECH_BANK")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.ALLOW_ALTERNATIVES)
                .alternativeTokenStrategy(BrandFailurePolicy.AlternativeTokenStrategy.PRIORITY_BASED)
                .requiredTokenFailureThreshold(2)
                .maxAlternativeAttempts(3)
                .tokenAlternatives(tokenAlternatives)
                .tokenGroups(tokenGroups)
                .fallbackGroups(Arrays.asList("PRIMARY_AUTH", "SECONDARY_AUTH"))
                .criticalTokens(Arrays.asList("MOBILE_PIN"))
                .allowPartialAuthentication(false) // Tech Bank requires full auth
                .partialAuthMinTokens(2)
                .failOnCriticalTokenFailure(false) // Allow alternatives
                .enableGracefulDegradation(true)
                .degradationThreshold(3)
                .build();
    }
}
```

## Step 4: Database/Data Model Extensions

### 4.1 Extend CustomerProfile

If your bank uses custom authentication data, extend the `CustomerProfile` model:

```java
// Add to CustomerProfile.java or create a custom extension
public class CustomerProfile {
    // Existing fields...
    
    // Tech Bank specific fields
    private String mobilePin;        // Hashed mobile PIN
    private String biometricId;      // Biometric identifier
    private String accountNumber;    // Account number for fallback
    
    // Getters and setters
    public String getMobilePin() { return mobilePin; }
    public void setMobilePin(String mobilePin) { this.mobilePin = mobilePin; }
    
    public String getBiometricId() { return biometricId; }
    public void setBiometricId(String biometricId) { this.biometricId = biometricId; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
}
```

### 4.2 Update Repository

Update your repository to handle the new fields:

```java
// In InMemoryCustomerProfileRepository.java or your database repository
@PostConstruct
public void initializeTestData() {
    // Create Tech Bank test customer
    CustomerProfile techBankCustomer = CustomerProfile.builder()
            .customerId("TECH001")
            .phoneNumber("+1555123456")
            .accountNumber("TB12345678")
            .mobilePin(EncryptionUtil.hash("123456"))  // Use simplified hash method
            .biometricId("BIOMETRIC_MATCH_001")
            .fullName("Tech Bank Customer")
            .email("customer@techbank.com")
            .accountStatus("ACTIVE")
            .build();
    
    storeCustomer(techBankCustomer);
}
```

## Step 5: Testing Your Implementation

### 5.1 Create Unit Tests

Create comprehensive tests for your validators:

```java
// File: src/test/java/com/bank/ivr/auth/validator/impl/TechBankMobilePinValidatorTest.java
package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TechBankMobilePinValidatorTest {
    
    private TechBankMobilePinValidator validator;
    private CustomerProfile customerProfile;
    
    @BeforeEach
    void setUp() {
        validator = new TechBankMobilePinValidator();
        customerProfile = CustomerProfile.builder()
                .customerId("TECH001")
                .mobilePin(EncryptionUtil.hash("123456"))
                .build();
    }
    
    @Test
    void shouldValidateCorrectMobilePin() {
        // When
        boolean result = validator.validate("TECH001", "123456", customerProfile);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldRejectIncorrectMobilePin() {
        // When
        boolean result = validator.validate("TECH001", "654321", customerProfile);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void shouldRejectInvalidFormat() {
        // When
        boolean result = validator.validate("TECH001", "12", customerProfile); // Too short
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void shouldNormalizeInput() {
        // When
        String normalized = validator.normalizeTokenValue("12-34-56");
        
        // Then
        assertEquals("123456", normalized);
    }
    
    @Test
    void shouldReturnCorrectTokenName() {
        assertEquals("MOBILE_PIN", validator.getTokenName());
    }
    
    @Test
    void shouldReturnCorrectBrand() {
        assertEquals("TECH_BANK", validator.getBrand());
    }
}
```

### 5.2 Create Integration Tests

Test the complete authentication flow:

```java
// File: src/test/java/com/bank/ivr/auth/integration/TechBankIntegrationTest.java
package com.bank.ivr.auth.integration;

import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.ProvidedToken;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase
class TechBankIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldAuthenticateTechBankCustomerSuccessfully() throws Exception {
        // Given
        CustomerIdentifier customerIdentifier = new CustomerIdentifier(
                CustomerIdentifier.IdentifierType.PHONE_NUMBER,
                "+1555123456"
        );
        
        AuthenticationRequest request = new AuthenticationRequest(
                "session-tech-001",
                customerIdentifier,
                null,
                Arrays.asList(
                        new ProvidedToken("MOBILE_PIN", "123456"),
                        new ProvidedToken("BIOMETRIC_ID", "BIOMETRIC_MATCH_001")
                ),
                "TECH_BANK"
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpected(jsonPath("$.message").value(containsString("Tech Bank")));
    }
}
```

### 5.3 Test Brand Configuration

Verify your brand configuration is loaded correctly:

```java
@Test
void shouldLoadTechBankConfiguration() {
    // When
    BrandAuthConfiguration config = brandConfigService.getBrandConfiguration("TECH_BANK");
    
    // Then
    assertNotNull(config);
    assertEquals("TECH_BANK", config.getBrandCode());
    assertEquals(2, config.getRequiredTokens().size());
    assertTrue(config.getRequiredTokens().contains("MOBILE_PIN"));
    assertTrue(config.getRequiredTokens().contains("BIOMETRIC_ID"));
}
```

## Step 6: Configuration and Deployment

### 6.1 Application Properties

Add any necessary configuration to `application.yml`:

```yaml
# Tech Bank specific configuration
tech-bank:
  biometric:
    service-url: "https://biometric-api.techbank.com"
    timeout: 5000
  mobile-pin:
    min-length: 4
    max-length: 6
    
# Logging configuration
logging:
  level:
    com.bank.ivr.auth.validator.impl.TechBank: DEBUG
```

### 6.2 Deployment Checklist

Before deploying to production:

- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Brand configuration is correct
- [ ] Test customer data is available
- [ ] Logging levels are appropriate
- [ ] Security review completed
- [ ] Performance testing completed

## Smart Token Re-asking Logic

The system includes intelligent token re-asking logic that prevents asking for tokens that have already failed validation:

### How It Works

1. **User provides wrong token**: System marks it as "validation failed"
2. **System won't re-ask**: That specific token won't be asked again
3. **Alternative tokens**: System asks for different tokens instead
4. **Smart fallback**: Uses priority-based selection for alternatives

### Example Flow

```
1. System asks for MOBILE_PIN
2. User provides wrong PIN → marked as failed
3. System asks for BIOMETRIC_ID (next priority)
4. User provides correct biometric → success
5. System never re-asks for MOBILE_PIN in this session
```

## Trust-Based Authentication (Advanced)

🆕 **New in v1.3**: The system now supports advanced trust-based authentication that can make conditional decisions based on trust levels and phone number matching status.

### Overview

Trust-based authentication allows banks to implement sophisticated authentication flows that adapt based on:

- **Trust Level**: RED (low trust) or GREEN (high trust) assigned by external systems
- **Phone Matching**: Whether the caller's phone number matches customer records
- **Match Count**: How many customer records match the phone number

### Key Components

#### 1. TrustLevelInfo Model

```java
public class TrustLevelInfo {
    public enum TrustLevel { RED, GREEN }
    
    public enum PhoneMatchStatus {
        NOT_MATCHED,           // Phone not matched with any SSN
        SINGLE_MATCH,          // Phone matched with exactly one SSN
        MULTIPLE_MATCHES       // Phone matched with multiple SSNs
    }
    
    private final TrustLevel trustLevel;
    private final PhoneMatchStatus phoneMatchStatus;
    private final int matchedSsnCount;
}
```

#### 2. Enhanced Authentication Request

All authentication requests now include trust level information:

```java
AuthenticationRequest request = new AuthenticationRequest(
    sessionId,
    customerIdentifier,
    attemptId,
    providedTokens,
    brand,
    trustLevelInfo  // New required parameter
);
```

#### 3. Conditional Authentication Rules

Implement `ConditionalAuthenticationRule` for complex logic:

```java
@Component
public class MyBankTrustBasedRule implements ConditionalAuthenticationRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        // Example: Trust Level GREEN + Phone not matched → Ask for SSN last 4
        if (trustInfo.getTrustLevel() == TrustLevel.GREEN && 
            trustInfo.getPhoneMatchStatus() == PhoneMatchStatus.NOT_MATCHED) {
            return "SSN_LAST_4";
        }
        
        // Example: Trust Level RED + Multiple matches → Ask for full SSN
        if (trustInfo.getTrustLevel() == TrustLevel.RED && 
            trustInfo.getPhoneMatchStatus() == PhoneMatchStatus.MULTIPLE_MATCHES) {
            return "SSN_FULL";
        }
        
        return null; // Use default logic
    }
    
    @Override
    public boolean shouldEscalateToken(String currentToken, AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        // Example: If SSN_LAST_4 fails and trust is GREEN, escalate to full SSN
        if ("SSN_LAST_4".equals(currentToken) && 
            trustInfo.getTrustLevel() == TrustLevel.GREEN &&
            context.hasAskedTokenValidationFailure("SSN_LAST_4")) {
            return true;
        }
        
        return false;
    }
}
```

### Royal Bank Example Implementation

The system includes a complete Royal Bank implementation demonstrating trust-based authentication:

#### Token Definitions
```java
// SSN Last 4 Digits - for high trust scenarios
AuthTokenDefinition.builder()
    .name("SSN_LAST_4")
    .displayName("Last 4 digits of Social Security Number")
    .description("Last 4 digits of SSN for lower risk authentication")
    .priority(100)
    .maxAttempts(2)
    .validationPattern("\\d{4}")
    .build()

// Full SSN - for low trust or high risk scenarios  
AuthTokenDefinition.builder()
    .name("SSN_FULL")
    .displayName("Full Social Security Number")
    .description("Complete SSN for higher risk authentication")
    .priority(95)
    .maxAttempts(1)
    .validationPattern("\\d{9}")
    .build()
```

#### Complex Conditional Logic
```java
@Override
public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
    TrustLevelInfo trustInfo = context.getTrustLevelInfo();
    
    // Scenario 1: Green trust + phone not matched → SSN last 4
    if (trustInfo.getTrustLevel() == TrustLevel.GREEN && 
        trustInfo.getPhoneMatchStatus() == PhoneMatchStatus.NOT_MATCHED) {
        return "SSN_LAST_4";
    }
    
    // Scenario 2: Red trust + multiple phone matches → Full SSN
    if (trustInfo.getTrustLevel() == TrustLevel.RED && 
        trustInfo.getPhoneMatchStatus() == PhoneMatchStatus.MULTIPLE_MATCHES) {
        return "SSN_FULL";
    }
    
    // Scenario 3: Green trust + single match → PIN only
    if (trustInfo.getTrustLevel() == TrustLevel.GREEN && 
        trustInfo.getPhoneMatchStatus() == PhoneMatchStatus.SINGLE_MATCH) {
        return "DEBIT_CARD_PIN";
    }
    
    // Default fallback
    return "SSN_LAST_4";
}
```

### Implementation Steps

#### 1. Update Authentication Requests

Ensure all authentication requests include trust level information:

```java
// In your service/controller
TrustLevelInfo trustInfo = new TrustLevelInfo(
    TrustLevel.GREEN,                    // From external trust system
    PhoneMatchStatus.SINGLE_MATCH,      // From phone matching service
    1                                    // Number of matches found
);

AuthenticationRequest request = new AuthenticationRequest(
    sessionId,
    customerIdentifier,
    attemptId,
    providedTokens,
    brand,
    trustInfo
);
```

#### 2. Create Trust-Based Rules

Implement conditional rules for your brand:

```java
@Component
public class MyBankTrustRule implements ConditionalAuthenticationRule {
    
    @Override
    public String getBrandCode() {
        return "MY_BANK";
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        // Implement your bank's trust-based logic
        return null;
    }
}
```

#### 3. Update Tests

Update your tests to include trust level information:

```java
private TrustLevelInfo createDefaultTrustLevelInfo() {
    return new TrustLevelInfo(
        TrustLevelInfo.TrustLevel.GREEN,
        TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
        1
    );
}

@Test
void testTrustBasedAuthentication() {
    AuthenticationRequest request = new AuthenticationRequest(
        sessionId,
        customerIdentifier,
        attemptId,
        providedTokens,
        brand,
        createDefaultTrustLevelInfo()
    );
    
    // Test your trust-based logic
}
```

### Benefits

- **Adaptive Security**: Higher security for lower trust scenarios
- **Better UX**: Fewer authentication steps for trusted users
- **Risk Management**: Dynamic authentication based on risk assessment
- **Scalability**: Supports hundreds of conditional scenarios

### Use Cases

1. **Fraud Prevention**: Red trust level triggers additional authentication
2. **Customer Experience**: Green trust level enables streamlined authentication
3. **Phone Verification**: Different flows based on phone number matching
4. **Progressive Authentication**: Escalate security based on context

For complete implementation details, see [ROYAL_BANK_TRUST_AUTHENTICATION.md](ROYAL_BANK_TRUST_AUTHENTICATION.md).

## Troubleshooting

### Common Issues

#### 1. Validator Not Found
**Error**: `No validator found for brand 'TECH_BANK' and token 'MOBILE_PIN'`

**Solution**: 
- Ensure your validator class is annotated with `@Component`
- Check that `getTokenName()` and `getBrand()` return correct values
- Verify the validator is in a package scanned by Spring

#### 2. Duplicate Validator Error
**Error**: `Multiple validators found for brand 'TECH_BANK' and token 'MOBILE_PIN'`

**Solution**:
- Only one validator per brand+token combination is allowed
- Remove duplicate validator classes
- Check for conflicting `@Component` annotations

#### 3. Compilation Errors
**Error**: Method not found errors

**Solution**:
- Use `EncryptionUtil.hash()` instead of deprecated `hashPin()`
- Use `EncryptionUtil.verify()` instead of deprecated `verifyPin()`
- Update import statements to be specific (avoid wildcards)

#### 4. Test Failures
**Error**: Tests expecting deprecated method calls

**Solution**:
- Update test expectations to match simplified controller behavior
- Remove verifications for methods that were removed during cleanup
- Focus tests on actual functionality rather than internal method calls

### Debug Tips

1. **Enable Debug Logging**:
```yaml
logging:
  level:
    com.bank.ivr.auth: DEBUG
```

2. **Check Validator Registration**:
Look for startup logs showing validator registration:
```
Registered validator for brand 'TECH_BANK', token 'MOBILE_PIN' with priority 150
```

3. **Verify Brand Configuration**:
```
Initialized BrandAuthConfigurationService with X brand configurations: [TECH_BANK, ...]
```

## Best Practices

### 1. Security Best Practices

- **Always hash sensitive data**: Use `EncryptionUtil.hash()` for PINs and passwords
- **Validate input format**: Implement proper regex validation
- **Log security events**: Log authentication attempts and failures
- **Use appropriate priorities**: Higher priority for more secure tokens

### 2. Code Quality

- **Use specific imports**: Avoid wildcard imports (`import java.util.*;`)
- **Follow naming conventions**: Clear, descriptive class and method names
- **Add comprehensive tests**: Unit tests for validators, integration tests for flows
- **Document custom logic**: Explain brand-specific business rules

### 3. Performance

- **Efficient validation**: Keep validation logic fast and simple
- **Avoid external calls**: Minimize network calls during validation
- **Cache when appropriate**: Cache expensive operations
- **Monitor performance**: Track validation times

### 4. Maintainability

- **Single responsibility**: Each validator handles one token type for one brand
- **Clear configuration**: Make brand rules explicit and documented
- **Version your changes**: Use proper version control and change documentation
- **Plan for migration**: Consider backward compatibility

## Migration Guide

### From Previous Versions

If you're migrating from an older version of the system:

#### 1. Update Encryption Calls
```java
// Old (deprecated - removed)
String hash = EncryptionUtil.hashPin(pin);
boolean valid = EncryptionUtil.verifyPin(plainPin, hashedPin);

// New (current)
String hash = EncryptionUtil.hash(pin);
boolean valid = EncryptionUtil.verify(plainPin, hashedPin);
```

#### 2. Update Import Statements
```java
// Old (avoid)
import java.util.*;
import com.bank.ivr.auth.model.domain.*;

// New (preferred)
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.CustomerProfile;
```

#### 3. Update Test Expectations
```java
// Old (may fail)
verify(brandConfigService).getMaxOverallAttemptsForBrand("TECH_BANK");
verify(brandConfigService).getRequiredTokensForBrand("TECH_BANK");

// New (current behavior)
verify(brandConfigService).isBrandSupported("TECH_BANK");
// Note: Other methods called by orchestrator, not controller
```

### Brand-Aware Migration

If migrating from non-brand-aware validators:

1. **Add brand support**: Implement `getBrand()` method
2. **Update registration**: Ensure unique brand+token combinations
3. **Test thoroughly**: Verify brand-specific behavior works correctly

---

## Conclusion

You now have a complete guide for onboarding a new bank to the IVR Authentication System. The system's brand-aware architecture allows for flexible, secure, and maintainable authentication solutions tailored to each bank's specific requirements.

For additional support or questions, refer to:
- [BEGINNER_GUIDE.md](BEGINNER_GUIDE.md) for system fundamentals
- [CODEBASE_CLEANUP_SUMMARY.md](CODEBASE_CLEANUP_SUMMARY.md) for recent changes
- System documentation and code comments
- Development team contacts

Happy coding! 🏦✨ 