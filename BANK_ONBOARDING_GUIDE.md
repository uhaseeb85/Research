# Bank Onboarding Guide - IVR Authentication System

This comprehensive guide walks you through the process of adding a new bank (brand) to the IVR Authentication System, including implementing custom authentication tokens and validation rules.

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
10. [Troubleshooting](#troubleshooting)
11. [Best Practices](#best-practices)

## Overview

The IVR Authentication System supports multiple banks (brands) with their own authentication requirements. Each bank can have:

- **Custom Token Types**: Unique authentication methods (PIN, SSN, biometrics, etc.)
- **Brand-Specific Validation Logic**: Different rules for validating the same token type
- **Custom Retry Policies**: Brand-specific attempt limits and lockout rules
- **Tailored User Experience**: Custom messages and prompts

### Key Architecture Components

- **TokenValidator**: Interface for implementing token validation logic
- **BrandAuthConfiguration**: Interface for defining brand-specific authentication rules
- **TokenValidationService**: Central service that manages all validators
- **CustomerProfile**: Data model containing customer authentication information

## Prerequisites

Before starting, ensure you have:

1. **Development Environment**: Java 11+, Spring Boot 2.7+, Maven
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
        // TODO: Implement your validation logic
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
        
        // TODO: Add your specific validation logic here
        // Examples:
        // - Hash comparison
        // - External API validation
        // - Business rule validation
        
        boolean isValid = normalizedProvided.equals(customerProfile.getMobilePin());
        
        if (isValid) {
            logger.debug("Mobile PIN validation successful for customer {}", customerIdentifierValue);
        } else {
            logger.debug("Mobile PIN validation failed for customer {}", customerIdentifierValue);
        }
        
        return isValid;
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

### 2.2 Create Additional Validators

For each token type your bank uses, create similar validator classes. Here's an example for a biometric ID validator:

```java
// File: src/main/java/com/bank/ivr/auth/validator/impl/TechBankBiometricValidator.java
package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TechBankBiometricValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(TechBankBiometricValidator.class);
    
    // TODO: Inject your biometric validation service
    // @Autowired
    // private BiometricValidationService biometricService;
    
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
        // TODO: Implement biometric validation logic
        // This might involve:
        // - Calling external biometric service
        // - Comparing biometric templates
        // - Validating biometric token format
        
        logger.debug("Biometric validation for customer {}", customerIdentifierValue);
        
        try {
            // Example implementation
            // return biometricService.validate(customerIdentifierValue, providedTokenValue);
            
            // Placeholder implementation
            return providedTokenValue != null && providedTokenValue.startsWith("BIO_");
        } catch (Exception e) {
            logger.error("Error validating biometric for customer {}: {}", customerIdentifierValue, e.getMessage());
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return 140;
    }
}
```

### 2.3 Validator Implementation Checklist

For each validator, ensure you implement:

- [ ] **getTokenName()**: Return the exact token name used in configuration
- [ ] **getBrand()**: Return your brand code
- [ ] **validate()**: Core validation logic with proper error handling
- [ ] **normalizeTokenValue()**: Input sanitization and formatting
- [ ] **getPriority()**: Appropriate priority for token ordering
- [ ] **Logging**: Debug and error logging for troubleshooting
- [ ] **Null Safety**: Handle null inputs gracefully
- [ ] **Exception Handling**: Catch and log validation errors

## Step 3: Create Brand Configuration

### 3.1 Implement BrandAuthConfiguration

Create a configuration class that defines your bank's authentication behavior:

```java
// File: src/main/java/com/bank/ivr/auth/config/impl/TechBankAuthConfiguration.java
package com.bank.ivr.auth.config.impl;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;
import com.bank.ivr.auth.model.domain.TokenRetryStrategy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Authentication configuration for Tech Bank.
 * Modern tech-focused bank with mobile-first authentication.
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
                    .priority(150)
                    .maskingRegex("\\d{4,6}")
                    .inputFormatRegex("^\\d{4,6}$")
                    .maxAttempts(3)
                    .build(),
            
            // Secondary: Biometric
            AuthTokenDefinition.builder()
                    .name("BIOMETRIC_ID")
                    .description("Biometric Authentication")
                    .priority(140)
                    .maskingRegex("BIO_\\*+")
                    .inputFormatRegex("^BIO_[A-Z0-9]{8,}$")
                    .maxAttempts(2)
                    .build(),
            
            // Fallback: Account Number
            AuthTokenDefinition.builder()
                    .name("ACCOUNT_NUMBER")
                    .description("Account Number")
                    .priority(130)
                    .maskingRegex("\\*{4,8}\\d{4}")
                    .inputFormatRegex("^\\d{8,16}$")
                    .maxAttempts(3)
                    .build(),
            
            // Security Question
            AuthTokenDefinition.builder()
                    .name("SECURITY_QUESTION")
                    .description("Security Question Answer")
                    .priority(120)
                    .maskingRegex("\\*{3,}")
                    .inputFormatRegex("^.{3,50}$")
                    .maxAttempts(2)
                    .build()
        );
    }
    
    @Override
    public List<String> getRequiredTokens() {
        // Tech Bank requires mobile PIN + one additional factor
        return Arrays.asList("MOBILE_PIN", "BIOMETRIC_ID"); // Alternative: ACCOUNT_NUMBER
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 5;
    }
    
    @Override
    public Map<String, Integer> getBrandSpecificTokenAttempts() {
        Map<String, Integer> attempts = new HashMap<>();
        attempts.put("MOBILE_PIN", 3);
        attempts.put("BIOMETRIC_ID", 2);
        attempts.put("ACCOUNT_NUMBER", 3);
        attempts.put("SECURITY_QUESTION", 2);
        return attempts;
    }
    
    @Override
    public boolean isConcurrentTokenAuthAllowed() {
        return true; // Tech-savvy customers can provide multiple tokens
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Tech Bank - Digital Banking Made Simple");
        messages.put("primary_prompt", "Please enter your {token_description}");
        messages.put("secondary_prompt", "For additional security, please provide your {token_description}");
        messages.put("success", "Authentication successful. Welcome to Tech Banking!");
        messages.put("failure", "Authentication failed. Contact Tech Support at 1-800-TECH-BANK or visit our app.");
        messages.put("customer_not_found", "Account not found. Please verify your information or contact support.");
        messages.put("session_expired", "Session expired. Please restart authentication.");
        messages.put("system_error", "Technical difficulties. Please try again or use our mobile app.");
        messages.put("no_methods", "No authentication methods available. Please contact support.");
        return messages;
    }
    
    @Override
    public Map<String, TokenRetryStrategy> getTokenRetryStrategies() {
        Map<String, TokenRetryStrategy> strategies = new HashMap<>();
        
        // Mobile PIN strategy
        strategies.put("MOBILE_PIN", TokenRetryStrategy.builder()
                .tokenName("MOBILE_PIN")
                .retryType(TokenRetryStrategy.RetryType.LINEAR_BACKOFF)
                .maxRetries(3)
                .baseDelayMs(2000) // 2 second delay
                .maxDelayMs(10000) // 10 second max
                .progressiveLockoutEnabled(true)
                .lockoutDurationAfterExhaustion(Duration.ofMinutes(10))
                .build());
                
        // Biometric strategy - longer delays for processing
        strategies.put("BIOMETRIC_ID", TokenRetryStrategy.builder()
                .tokenName("BIOMETRIC_ID")
                .retryType(TokenRetryStrategy.RetryType.EXPONENTIAL_BACKOFF)
                .maxRetries(2)
                .baseDelayMs(5000) // 5 second base
                .maxDelayMs(20000) // 20 second max
                .multiplier(2.0)
                .progressiveLockoutEnabled(true)
                .lockoutDurationAfterExhaustion(Duration.ofMinutes(15))
                .build());
                
        return strategies;
    }
    
    @Override
    public BrandGlobalRetryPolicy getGlobalRetryPolicy() {
        return BrandGlobalRetryPolicy.builder()
                .brandCode("TECH_BANK")
                .maxGlobalAttempts(6) // Moderate limit
                .globalLockoutEnabled(true)
                .globalLockoutThreshold(5)
                .globalLockoutDuration(Duration.ofMinutes(15))
                .escalationPolicy(BrandGlobalRetryPolicy.EscalationPolicy.LINEAR_DELAY)
                .escalationThreshold(4)
                .crossTokenDelayEnabled(false) // Fast experience for tech users
                .suspiciousActivityThreshold(5)
                .suspiciousActivityLockoutDuration(Duration.ofMinutes(20))
                .retryWindowResetDuration(Duration.ofHours(1))
                .enableRetryAnalytics(true)
                .build();
    }
    
    @Override
    public int getPriority() {
        return 80; // Standard priority
    }
}
```

### 3.2 Configuration Checklist

Ensure your configuration includes:

- [ ] **Unique Brand Code**: No conflicts with existing brands
- [ ] **Complete Token Definitions**: All tokens with proper validation rules
- [ ] **Required Token List**: Minimum tokens needed for authentication
- [ ] **Attempt Limits**: Reasonable limits that balance security and usability
- [ ] **Custom Messages**: User-friendly, brand-appropriate text
- [ ] **Retry Strategies**: Appropriate delays and lockout policies
- [ ] **Global Retry Policy**: Overall security enforcement

## Step 4: Database/Data Model Extensions

### 4.1 Extend CustomerProfile (if needed)

If your bank uses custom data fields not in the existing `CustomerProfile`, extend the model:

```java
// Add to CustomerProfile.java or create a custom extension
public class CustomerProfile {
    // ... existing fields ...
    
    // Tech Bank specific fields
    private String mobilePin;           // For MOBILE_PIN token
    private String biometricTemplate;   // For BIOMETRIC_ID token
    private String securityAnswer;      // For SECURITY_QUESTION token
    private String preferredAuthMethod; // User preference
    
    // Add getters and setters
    public String getMobilePin() {
        return mobilePin;
    }
    
    public void setMobilePin(String mobilePin) {
        this.mobilePin = mobilePin;
    }
    
    public String getBiometricTemplate() {
        return biometricTemplate;
    }
    
    public void setBiometricTemplate(String biometricTemplate) {
        this.biometricTemplate = biometricTemplate;
    }
    
    public String getSecurityAnswer() {
        return securityAnswer;
    }
    
    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }
    
    public String getPreferredAuthMethod() {
        return preferredAuthMethod;
    }
    
    public void setPreferredAuthMethod(String preferredAuthMethod) {
        this.preferredAuthMethod = preferredAuthMethod;
    }
    
    // Update builder pattern
    public static class Builder {
        // ... existing fields ...
        private String mobilePin;
        private String biometricTemplate;
        private String securityAnswer;
        private String preferredAuthMethod;
        
        public Builder mobilePin(String mobilePin) {
            this.mobilePin = mobilePin;
            return this;
        }
        
        public Builder biometricTemplate(String biometricTemplate) {
            this.biometricTemplate = biometricTemplate;
            return this;
        }
        
        public Builder securityAnswer(String securityAnswer) {
            this.securityAnswer = securityAnswer;
            return this;
        }
        
        public Builder preferredAuthMethod(String preferredAuthMethod) {
            this.preferredAuthMethod = preferredAuthMethod;
            return this;
        }
        
        // Update build() method to include new fields
    }
}
```

### 4.2 Database Schema Updates

If using a database, create migration scripts for new fields:

```sql
-- Example migration for Tech Bank fields
ALTER TABLE customer_profiles 
ADD COLUMN mobile_pin VARCHAR(255),
ADD COLUMN biometric_template TEXT,
ADD COLUMN security_answer VARCHAR(255),
ADD COLUMN preferred_auth_method VARCHAR(50);

-- Add indexes for performance
CREATE INDEX idx_customer_mobile_pin ON customer_profiles(mobile_pin);
CREATE INDEX idx_customer_preferred_auth ON customer_profiles(preferred_auth_method);
```

## Step 5: Testing Your Implementation

### 5.1 Create Unit Tests

Test each validator individually:

```java
// File: src/test/java/com/bank/ivr/auth/validator/impl/TechBankMobilePinValidatorTest.java
package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
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
                .customerId("TEST123")
                .mobilePin("1234")
                .build();
    }
    
    @Test
    void testValidateSuccess() {
        boolean result = validator.validate("TEST123", "1234", customerProfile);
        assertTrue(result);
    }
    
    @Test
    void testValidateWrongPin() {
        boolean result = validator.validate("TEST123", "5678", customerProfile);
        assertFalse(result);
    }
    
    @Test
    void testValidateNullPin() {
        boolean result = validator.validate("TEST123", null, customerProfile);
        assertFalse(result);
    }
    
    @Test
    void testNormalizeTokenValue() {
        assertEquals("1234", validator.normalizeTokenValue("1234"));
        assertEquals("1234", validator.normalizeTokenValue("1-2-3-4"));
        assertEquals("1234", validator.normalizeTokenValue("1 2 3 4"));
        assertEquals("", validator.normalizeTokenValue("abcd"));
    }
    
    @Test
    void testGetTokenName() {
        assertEquals("MOBILE_PIN", validator.getTokenName());
    }
    
    @Test
    void testGetBrand() {
        assertEquals("TECH_BANK", validator.getBrand());
    }
}
```

### 5.2 Create Integration Tests

Test the complete authentication flow:

```java
// File: src/test/java/com/bank/ivr/auth/service/TechBankIntegrationTest.java
package com.bank.ivr.auth.service;

import com.bank.ivr.auth.config.impl.TechBankAuthConfiguration;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TechBankIntegrationTest {
    
    @Autowired
    private TokenValidationService tokenValidationService;
    
    @Autowired
    private TechBankAuthConfiguration techBankConfig;
    
    @Test
    void testTechBankMobilePinValidation() {
        CustomerProfile customer = CustomerProfile.builder()
                .customerId("TECH001")
                .mobilePin("123456")
                .build();
                
        boolean result = tokenValidationService.validateToken(
                "MOBILE_PIN", 
                "TECH_BANK", 
                "TECH001", 
                "123456", 
                customer);
                
        assertTrue(result);
    }
    
    @Test
    void testTechBankConfigurationLoaded() {
        assertEquals("TECH_BANK", techBankConfig.getBrandCode());
        assertEquals(4, techBankConfig.getTokenDefinitions().size());
        assertTrue(techBankConfig.getRequiredTokens().contains("MOBILE_PIN"));
    }
    
    @Test
    void testValidatorRegistration() {
        assertTrue(tokenValidationService.hasValidator("TECH_BANK", "MOBILE_PIN"));
        assertTrue(tokenValidationService.hasValidator("TECH_BANK", "BIOMETRIC_ID"));
    }
}
```

### 5.3 Manual Testing Script

Create a PowerShell script for manual testing:

```powershell
# File: test-tech-bank.ps1
$baseUrl = "http://localhost:8080"

# Test Tech Bank Mobile PIN validation
$testData = @{
    brand = "TECH_BANK"
    customerIdentifier = "TECH001"
    tokenName = "MOBILE_PIN"
    tokenValue = "123456"
}

$response = Invoke-RestMethod -Uri "$baseUrl/auth/validate" -Method Post -Body ($testData | ConvertTo-Json) -ContentType "application/json"

Write-Host "Tech Bank Mobile PIN Test Result: $($response.success)"
```

## Step 6: Configuration and Deployment

### 6.1 Application Configuration

Update `application.yml` if needed for brand-specific settings:

```yaml
# Add to application.yml
app:
  auth:
    brands:
      tech-bank:
        enabled: true
        customer-lookup-timeout: 5s
        external-validation-timeout: 10s
        biometric-service-url: "https://api.techbank.com/biometric"
        
  # Custom logging for your brand
logging:
  level:
    com.bank.ivr.auth.validator.impl.TechBank: DEBUG
    com.bank.ivr.auth.config.impl.TechBankAuthConfiguration: DEBUG
```

### 6.2 Environment-Specific Configuration

Create environment-specific configuration files:

```yaml
# application-dev.yml
app:
  auth:
    brands:
      tech-bank:
        biometric-service-url: "https://dev-api.techbank.com/biometric"
        mock-biometric-validation: true

# application-prod.yml
app:
  auth:
    brands:
      tech-bank:
        biometric-service-url: "https://api.techbank.com/biometric"
        mock-biometric-validation: false
        additional-security-headers: true
```

### 6.3 Build and Deployment

1. **Build the application:**
```bash
mvn clean compile
```

2. **Run tests:**
```bash
mvn test
```

3. **Package:**
```bash
mvn package
```

4. **Deploy to environment:**
```bash
java -jar target/bank-ivr-auth-system.jar --spring.profiles.active=prod
```

## Smart Token Re-asking Logic

### Overview

The IVR Authentication System includes intelligent token re-asking logic that prevents unnecessary re-prompting of tokens and improves user experience. This system tracks which tokens have been asked and whether users provided them, implementing smart decisions about when to re-ask tokens.

### Key Features

#### 1. **Asked Token Tracking**
The system tracks all tokens that have been asked during an authentication session:

```java
// In AuthenticationContext
private List<String> askedTokens = new ArrayList<>();
private Map<String, Integer> askedTokensWithValidationFailure = new HashMap<>();

// Methods for tracking
public void addAskedToken(String tokenName);
public boolean hasAskedToken(String tokenName);
public void markAskedTokenValidationFailure(String tokenName);
public boolean hasAskedTokenValidationFailure(String tokenName);
```

#### 2. **Smart Re-asking Rules**

The system follows these intelligent rules:

- **✅ Can Re-ask**: Token was asked but user didn't provide it (no validation attempt)
- **❌ Cannot Re-ask**: Token was asked, user provided it, but validation failed
- **❌ Cannot Re-ask**: Token has completely failed (exhausted all attempts)

#### 3. **Implementation in Your Brand**

When implementing your brand configuration, consider these re-asking behaviors:

```java
@Component
public class TechBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            // Primary token - will be asked first
            AuthTokenDefinition.builder()
                    .name("MOBILE_PIN")
                    .description("Mobile Banking PIN")
                    .priority(150) // Highest priority
                    .maxAttempts(3)
                    .build(),
            
            // Secondary token - asked if primary fails or as backup
            AuthTokenDefinition.builder()
                    .name("BIOMETRIC_ID")
                    .description("Biometric Authentication")
                    .priority(140)
                    .maxAttempts(2)
                    .build()
        );
    }
}
```

### Authentication Flow Examples

#### Example 1: User Doesn't Provide Asked Token

```
1. System asks: "Please provide your Mobile PIN"
   → Token "MOBILE_PIN" added to askedTokens
   
2. User provides: SSN instead of PIN
   → System validates SSN successfully
   → MOBILE_PIN can still be re-asked (user never provided it)
   
3. System asks: "Please also provide your Mobile PIN for additional security"
   → Re-asking is allowed because user never attempted PIN
```

#### Example 2: User Provides Wrong Token

```
1. System asks: "Please provide your Mobile PIN"
   → Token "MOBILE_PIN" added to askedTokens
   
2. User provides: Wrong PIN "9999"
   → System validates PIN, fails
   → MOBILE_PIN marked with validation failure
   → Cannot re-ask MOBILE_PIN (user provided it but failed)
   
3. System asks: "Please provide your Biometric ID"
   → Moves to next available token
```

#### Example 3: Token Completely Failed

```
1. User provides wrong PIN 3 times
   → PIN exhausts all attempts
   → PIN added to failedTokens
   → PIN cannot be re-asked or selected
   
2. System excludes PIN from all future selections
   → Primary token selection skips PIN
   → Secondary token lists exclude PIN
```

### Testing Smart Re-asking Logic

When testing your brand implementation, verify these scenarios:

```java
@Test
@DisplayName("Should allow re-asking token that was asked but user didn't provide")
void shouldAllowReAskingUnprovidedToken() {
    // Given - token was asked but user didn't provide it
    context.addAskedToken("MOBILE_PIN");
    // No validation failure marked
    
    // When
    AuthenticationResponse response = authService.buildResponse(context, profile, "TECH_BANK");
    
    // Then
    assertEquals("MOBILE_PIN", response.getPrimaryTokenToAsk().getName());
    assertTrue(context.canReAskToken("MOBILE_PIN"));
}

@Test
@DisplayName("Should not re-ask token that user provided but failed validation")
void shouldNotReAskFailedToken() {
    // Given - token was asked and user provided it but validation failed
    context.addAskedToken("MOBILE_PIN");
    context.markAskedTokenValidationFailure("MOBILE_PIN");
    
    // When
    AuthenticationResponse response = authService.buildResponse(context, profile, "TECH_BANK");
    
    // Then
    assertNotEquals("MOBILE_PIN", response.getPrimaryTokenToAsk().getName());
    assertFalse(context.canReAskToken("MOBILE_PIN"));
}
```

### Configuration Considerations

#### Token Priority Planning

Plan your token priorities considering re-asking logic:

```java
// High priority tokens will be asked first
// If they fail validation, system moves to lower priority tokens
// Plan fallback chains accordingly

AuthTokenDefinition.builder()
    .name("PRIMARY_TOKEN")
    .priority(150)  // Asked first
    .maxAttempts(3)
    .build(),

AuthTokenDefinition.builder()
    .name("SECONDARY_TOKEN") 
    .priority(140)  // Asked if primary fails
    .maxAttempts(2)
    .build(),

AuthTokenDefinition.builder()
    .name("BACKUP_TOKEN")
    .priority(130)  // Last resort
    .maxAttempts(3)
    .build()
```

#### Required Tokens Strategy

Consider how re-asking logic affects required tokens:

```java
@Override
public List<String> getRequiredTokens() {
    // If a required token fails completely, authentication may fail
    // Plan alternative required token combinations
    return Arrays.asList("MOBILE_PIN", "BIOMETRIC_ID"); // Both required
    
    // Alternative: Allow either/or
    // return Arrays.asList("MOBILE_PIN"); // Only one required
}
```

### Benefits for Your Brand

1. **Improved User Experience**: Users aren't repeatedly asked for tokens they already failed
2. **Reduced Frustration**: Smart logic prevents unnecessary re-prompting
3. **Better Security**: Failed tokens are properly tracked and excluded
4. **Flexible Authentication**: System adapts to user behavior and token availability

### Integration with Brand Messages

Customize messages based on re-asking context:

```java
@Override
public Map<String, String> getBrandMessages() {
    Map<String, String> messages = new HashMap<>();
    messages.put("primary_prompt", "Please provide your {token_description}");
    messages.put("retry_prompt", "Let's try a different method. Please provide your {token_description}");
    messages.put("alternative_prompt", "For additional security, please also provide your {token_description}");
    return messages;
}
```

The system will automatically choose appropriate messages based on whether a token is being asked for the first time, re-asked, or requested as an alternative.

## Troubleshooting

### Common Issues and Solutions

#### 1. Validator Not Found
**Error:** `No validator found for brand 'TECH_BANK' and token 'MOBILE_PIN'`

**Solution:**
- Ensure your validator class is annotated with `@Component`
- Verify the `getTokenName()` and `getBrand()` methods return correct values
- Check that the validator is in a package scanned by Spring

#### 2. Multiple Validators for Same Brand/Token
**Error:** `Multiple validators found for brand 'TECH_BANK' and token 'MOBILE_PIN'`

**Solution:**
- Each brand/token combination can only have one validator
- Check for duplicate validator implementations
- Remove or rename conflicting validators

#### 3. Configuration Not Loaded
**Error:** Brand configuration not being applied

**Solution:**
- Ensure configuration class is annotated with `@Component`
- Verify the `getBrandCode()` method returns the correct brand
- Check Spring component scanning configuration

#### 4. Authentication Always Fails
**Error:** Valid tokens being rejected

**Solution:**
- Add debug logging to your validator
- Check the `normalizeTokenValue()` method
- Verify customer profile data structure
- Test token validation logic separately

### Debug Logging

Enable detailed logging for troubleshooting:

```yaml
logging:
  level:
    com.bank.ivr.auth: DEBUG
    com.bank.ivr.auth.service.TokenValidationService: TRACE
    com.bank.ivr.auth.validator.impl: TRACE
```

### Validation Checklist

Before going live, verify:

- [ ] All validators implement required methods correctly
- [ ] Brand configuration is complete and loaded
- [ ] Unit tests pass for all components
- [ ] Integration tests cover main authentication flows
- [ ] Error handling works correctly
- [ ] Logging provides adequate troubleshooting information
- [ ] Security requirements are met
- [ ] Performance is acceptable under load
- [ ] Mock setups in tests use `lenient()` for unused stubs to avoid unnecessary stubbing errors

## Best Practices

### Security Best Practices

1. **Never Log Sensitive Data**
   - Don't log actual PIN values, SSNs, or other sensitive tokens
   - Use customer identifiers only in logs
   - Implement proper log sanitization

2. **Input Validation**
   - Always validate and sanitize input in `normalizeTokenValue()`
   - Implement proper format validation
   - Protect against injection attacks

3. **Error Handling**
   - Never expose internal error details to users
   - Log detailed errors for debugging
   - Return generic failure messages

4. **Encryption**
   - Use proper encryption for stored sensitive data
   - Never store plain text PINs or passwords
   - Use secure hashing algorithms

### Performance Best Practices

1. **Efficient Validation**
   - Keep validation logic lightweight
   - Cache expensive operations when appropriate
   - Set reasonable timeouts for external services

2. **Database Optimization**
   - Add proper indexes for customer lookups
   - Use connection pooling
   - Consider read replicas for high load

3. **Monitoring**
   - Implement metrics for validation performance
   - Monitor error rates by brand and token type
   - Set up alerts for unusual patterns

### Code Quality Best Practices

1. **Consistent Naming**
   - Use clear, descriptive class and method names
   - Follow established naming conventions
   - Use consistent brand prefixes

2. **Documentation**
   - Document all public methods
   - Explain complex validation logic
   - Keep documentation up to date

3. **Testing**
   - Aim for high test coverage
   - Test both positive and negative cases
   - Include edge cases and error scenarios
   - Use `lenient()` for mock setups that aren't used in all test methods to avoid unnecessary stubbing errors

### Maintenance Best Practices

1. **Version Control**
   - Use semantic versioning for your validators
   - Tag releases properly
   - Maintain clear commit messages

2. **Backward Compatibility**
   - Consider existing customers when making changes
   - Implement gradual rollouts for breaking changes
   - Maintain fallback mechanisms

3. **Monitoring and Alerting**
   - Monitor authentication success rates
   - Alert on unusual failure patterns
   - Track performance metrics

## Conclusion

Following this guide, you should now have a complete implementation of a new bank brand in the IVR Authentication System. Remember to:

1. **Start Simple**: Begin with basic validators and expand functionality gradually
2. **Test Thoroughly**: Comprehensive testing prevents production issues
3. **Monitor Continuously**: Keep track of performance and user experience
4. **Iterate Based on Feedback**: Improve based on real-world usage

For additional support or questions, refer to the system documentation or contact the development team.

---

**Next Steps:**
- Review the [BRAND_VALIDATION_GUIDE.md](BRAND_VALIDATION_GUIDE.md) for advanced validation techniques
- Check [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for API reference
- See [TEST_COVERAGE_SUMMARY.md](TEST_COVERAGE_SUMMARY.md) for testing best practices 