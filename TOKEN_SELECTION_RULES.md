# Token Selection Rules Documentation

## Overview

The IVR Authentication system uses a sophisticated, multi-layered approach to determine which authentication tokens to ask customers based on success and failure scenarios. This document explains where these complex rules are defined and how they work together.

## Architecture Overview

The token selection rules are implemented across **four main layers**:

1. **Brand-Specific Token Definitions & Priorities**
2. **Brand Failure Policies** 
3. **Smart Re-asking Logic**
4. **Core Token Selection Algorithm**

---

## 1. Brand-Specific Token Definitions & Priorities

### Location
`src/main/java/com/bank/ivr/auth/config/impl/` (e.g., `PremiumBankAuthConfiguration.java`)

### Purpose
Defines which tokens are available for each brand and their priority order.

### Example Configuration

```java
@Component
public class PremiumBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            // Primary token - highest priority
            AuthTokenDefinition.builder()
                    .name("SSN")
                    .description("Social Security Number")
                    .priority(100)  // ← Priority determines order
                    .maxAttempts(2)
                    .maskingRegex("\\d{3}-\\d{2}-\\d{4}")
                    .inputFormatRegex("^\\d{3}-?\\d{2}-?\\d{4}$")
                    .build(),
            
            // Secondary token
            AuthTokenDefinition.builder()
                    .name("DEBIT_CARD_PIN")
                    .description("Debit Card PIN")
                    .priority(90)   // ← Lower priority = asked later
                    .maxAttempts(3)
                    .maskingRegex("\\d{4}")
                    .inputFormatRegex("^\\d{4}$")
                    .build(),
                    
            // Tertiary token
            AuthTokenDefinition.builder()
                    .name("DATE_OF_BIRTH")
                    .description("Date of Birth")
                    .priority(80)   // ← Lowest priority
                    .maxAttempts(2)
                    .maskingRegex("\\d{2}/\\d{2}/\\d{4}")
                    .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$")
                    .build()
        );
    }
    
    @Override
    public List<String> getRequiredTokens() {
        // Premium Bank requires SSN and one additional factor
        return Arrays.asList("SSN", "DEBIT_CARD_PIN");
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 3; // Strict security for premium customers
    }
}
```

### Key Properties

- **Priority**: Higher numbers = asked first (100 > 90 > 80)
- **MaxAttempts**: How many times a token can fail before being marked as failed
- **Required Tokens**: Tokens that must be authenticated for full authentication
- **Brand-Specific Limits**: Override default attempt limits per brand

---

## 2. Brand Failure Policies

### Location
`src/main/java/com/bank/ivr/auth/model/domain/BrandFailurePolicy.java`

### Purpose
Defines what happens when tokens fail and how to select alternative tokens.

### Failure Strategies

```java
public enum FailureStrategy {
    FAIL_IMMEDIATELY,           // Fail as soon as any required token fails
    ALLOW_ALTERNATIVES,         // Try alternative tokens when required tokens fail
    REQUIRE_ALL_ATTEMPTED,      // Only fail after all eligible tokens have been tried
    PROGRESSIVE_FALLBACK        // Use fallback token groups in order
}
```

### Alternative Token Strategies

```java
public enum AlternativeTokenStrategy {
    ANY_REMAINING,              // Any remaining eligible token can be used
    PREDEFINED_ALTERNATIVES,    // Only specific alternative tokens allowed
    PRIORITY_BASED,             // Use next highest priority token
    GROUP_BASED                 // Use alternative token groups
}
```

### Example Policy Configuration

```java
@Override
public BrandFailurePolicy getBrandFailurePolicy() {
    return BrandFailurePolicy.builder()
            .brandCode("PREMIUM_BANK")
            .failureStrategy(FailureStrategy.ALLOW_ALTERNATIVES)
            .alternativeTokenStrategy(AlternativeTokenStrategy.PREDEFINED_ALTERNATIVES)
            .tokenAlternatives(Map.of(
                "SSN", List.of("DEBIT_CARD_PIN", "DATE_OF_BIRTH"),
                "DEBIT_CARD_PIN", List.of("SSN", "ACCOUNT_NUMBER"),
                "DATE_OF_BIRTH", List.of("SSN", "MOTHER_MAIDEN_NAME")
            ))
            .tokenGroups(Map.of(
                "PRIMARY_GROUP", List.of("SSN", "DEBIT_CARD_PIN"),
                "SECONDARY_GROUP", List.of("DATE_OF_BIRTH", "ACCOUNT_NUMBER"),
                "FALLBACK_GROUP", List.of("MOTHER_MAIDEN_NAME", "PHONE_VERIFICATION")
            ))
            .fallbackGroups(List.of("PRIMARY_GROUP", "SECONDARY_GROUP", "FALLBACK_GROUP"))
            .allowPartialAuthentication(false)
            .failOnCriticalTokenFailure(true)
            .criticalTokens(List.of("SSN"))
            .build();
}
```

---

## 3. Smart Re-asking Logic

### Location
`src/main/java/com/bank/ivr/auth/model/domain/TokenState.java`

### Purpose
Prevents asking for the same token twice if the user provided it but validation failed.

### Core Logic

```java
public boolean canReAskToken(String tokenName) {
    // Smart re-asking logic: don't re-ask tokens that have validation failures
    // If a user provided a token and it failed validation, never re-ask it
    if (hasAskedTokenValidationFailure(tokenName)) {
        return false;
    }
    
    // If no validation failure, can re-ask
    return true;
}
```

### How It Works

1. **Token Asked**: System asks for SSN
2. **User Provides Wrong SSN**: `markAskedTokenValidationFailure("SSN")` is called
3. **Future Requests**: `canReAskToken("SSN")` returns `false`
4. **Result**: System will never ask for SSN again in this session

### Benefits

- **Better UX**: No frustrating repeated requests for failed tokens
- **Security**: Failed tokens are tracked and avoided
- **Efficiency**: Faster authentication by trying different tokens
- **Flexibility**: System adapts to what the customer can provide

---

## 4. Core Token Selection Algorithm

### Location
`src/main/java/com/bank/ivr/auth/service/AuthenticationResponseService.java`

### Purpose
Implements the main logic for selecting the next token to ask.

### Algorithm Flow

```java
private AuthTokenDefinition determineNextToken(AuthenticationContext context, 
                                             List<AuthTokenDefinition> brandTokenDefinitions) {
    AuthTokenDefinition bestToken = null;
    int highestPriority = -1;
    
    // Loop through all token definitions to find the best one
    for (AuthTokenDefinition token : brandTokenDefinitions) {
        // Apply multiple filters:
        
        // 1. Must be eligible for this customer
        if (!context.getEligibleTokens().contains(token.getName())) continue;
        
        // 2. Must not already be authenticated
        if (context.isTokenAuthenticated(token.getName())) continue;
        
        // 3. Must not have failed completely
        if (context.isTokenFailed(token.getName())) continue;
        
        // 4. Must have remaining attempts
        if (!context.hasRemainingAttemptsForToken(token.getName())) continue;
        
        // 5. Smart re-asking logic: don't re-ask tokens that failed validation
        if (!context.canReAskToken(token.getName())) continue;
        
        // 6. Select highest priority token
        if (token.getPriority() > highestPriority) {
            bestToken = token;
            highestPriority = token.getPriority();
        }
    }
    
    return bestToken; // Will be null if no valid token found
}
```

### Alternative Token Selection

When no primary tokens are available, the system uses the Brand Failure Policy to find alternatives:

```java
// Try to get alternative token based on failure policy
AuthTokenDefinition nextToken = failurePolicyService.getNextAlternativeToken(
    context, brand, brandTokenDefinitions);

if (nextToken == null) {
    // Check if partial authentication is allowed
    if (failurePolicyService.isPartialAuthenticationAllowed(context, brand)) {
        // Allow partial authentication
        return AuthenticationResponse.partialSuccess();
    } else {
        // No alternatives and no partial auth - fail
        return AuthenticationResponse.failed();
    }
}
```

---

## Complete Flow Example

### Scenario: Premium Bank Customer Authentication

#### Initial Setup
```java
// Brand Configuration
PremiumBankAuthConfiguration:
- SSN (priority: 100, maxAttempts: 2)
- DEBIT_CARD_PIN (priority: 90, maxAttempts: 3)  
- DATE_OF_BIRTH (priority: 80, maxAttempts: 2)

// Failure Policy
BrandFailurePolicy:
- Strategy: ALLOW_ALTERNATIVES
- Alternative Strategy: PRIORITY_BASED
- Required Tokens: ["SSN", "DEBIT_CARD_PIN"]
```

#### Authentication Flow

```
🎯 Step 1: Initial Request
   → System asks for SSN (highest priority: 100)
   → Message: "Please provide your Social Security Number"

❌ Step 2: User provides wrong SSN
   → SSN validation fails
   → markAskedTokenValidationFailure("SSN") called
   → canReAskToken("SSN") = false
   → SSN attempts: 2 → 1
   → System asks for DEBIT_CARD_PIN (next highest priority: 90)
   → Message: "Please provide your Debit Card PIN"

✅ Step 3: User provides correct PIN
   → PIN authenticated successfully
   → System needs one more required token
   → SSN cannot be re-asked (smart re-asking logic)
   → System asks for DATE_OF_BIRTH (next available priority: 80)
   → Message: "Please provide your Date of Birth"

✅ Step 4: User provides correct DOB
   → DOB authenticated successfully
   → Required tokens satisfied: PIN ✅, DOB ✅ (SSN failed but alternatives worked)
   → Authentication successful!

Alternative Scenario:
❌ Step 4: User provides wrong DOB
   → DOB validation fails
   → markAskedTokenValidationFailure("DOB") called
   → No more tokens available with sufficient priority
   → Check failure policy alternatives
   → No valid alternatives found
   → Authentication fails
```

---

## How to Define New Rules

### 1. Create a New Brand Configuration

```java
@Component
public class TechBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public String getBrandCode() {
        return "TECH_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            // Modern authentication - biometric first
            AuthTokenDefinition.builder()
                    .name("BIOMETRIC")
                    .description("Biometric Authentication")
                    .priority(150)  // Highest priority
                    .maxAttempts(2)
                    .build(),
                    
            // Mobile-first approach
            AuthTokenDefinition.builder()
                    .name("MOBILE_PIN")
                    .description("Mobile Banking PIN")
                    .priority(120)  // Second priority
                    .maxAttempts(3)
                    .build(),
                    
            // Traditional fallback
            AuthTokenDefinition.builder()
                    .name("ACCOUNT_NUMBER")
                    .description("Account Number")
                    .priority(100)  // Fallback priority
                    .maxAttempts(2)
                    .build()
        );
    }
    
    @Override
    public List<String> getRequiredTokens() {
        // Tech Bank requires biometric and one additional factor
        return Arrays.asList("BIOMETRIC", "MOBILE_PIN");
    }
    
    @Override
    public BrandFailurePolicy getBrandFailurePolicy() {
        return BrandFailurePolicy.builder()
                .brandCode("TECH_BANK")
                .failureStrategy(FailureStrategy.PROGRESSIVE_FALLBACK)
                .alternativeTokenStrategy(AlternativeTokenStrategy.GROUP_BASED)
                .tokenGroups(Map.of(
                    "MODERN_AUTH", List.of("BIOMETRIC", "MOBILE_PIN"),
                    "TRADITIONAL_AUTH", List.of("ACCOUNT_NUMBER", "SSN"),
                    "EMERGENCY_AUTH", List.of("PHONE_VERIFICATION", "EMAIL_VERIFICATION")
                ))
                .fallbackGroups(List.of("MODERN_AUTH", "TRADITIONAL_AUTH", "EMERGENCY_AUTH"))
                .allowPartialAuthentication(true)
                .partialAuthMinTokens(1)
                .build();
    }
}
```

### 2. Define Complex Alternative Logic

```java
// Predefined alternatives for specific tokens
.tokenAlternatives(Map.of(
    "BIOMETRIC", List.of("MOBILE_PIN", "ACCOUNT_NUMBER"),
    "MOBILE_PIN", List.of("ACCOUNT_NUMBER", "SSN"),
    "ACCOUNT_NUMBER", List.of("SSN", "PHONE_VERIFICATION")
))

// Group-based fallback with progressive degradation
.tokenGroups(Map.of(
    "HIGH_SECURITY", List.of("BIOMETRIC", "MOBILE_PIN"),
    "MEDIUM_SECURITY", List.of("SSN", "DEBIT_CARD_PIN"),
    "LOW_SECURITY", List.of("DATE_OF_BIRTH", "MOTHER_MAIDEN_NAME"),
    "EMERGENCY", List.of("PHONE_VERIFICATION", "EMAIL_VERIFICATION")
))
.fallbackGroups(List.of("HIGH_SECURITY", "MEDIUM_SECURITY", "LOW_SECURITY", "EMERGENCY"))
```

### 3. Custom Token Validators

```java
@Component
public class BiometricValidator implements TokenValidator {
    
    @Override
    public String getTokenName() {
        return "BIOMETRIC";
    }
    
    @Override
    public String getBrand() {
        return "TECH_BANK";
    }
    
    @Override
    public boolean validate(String customerId, String biometricData, CustomerProfile profile) {
        // Call external biometric service
        return biometricService.verify(biometricData, profile.getBiometricId());
    }
}
```

---

## Key Benefits

### 🎯 **Brand-Specific Customization**
- Each bank can have completely different token selection logic
- Different priorities, attempt limits, and failure strategies per brand
- Flexible configuration without code changes

### 🧠 **Smart Re-asking Prevention**
- Never asks for the same token twice if it failed validation
- Improves customer experience by avoiding frustrating repeated requests
- Tracks validation failures separately from attempt failures

### 📊 **Priority-Based Selection**
- Always asks highest priority available token first
- Ensures optimal authentication flow for each brand
- Supports complex priority hierarchies

### 🔄 **Flexible Failure Handling**
- Multiple strategies for handling token failures
- Predefined alternatives, group-based fallbacks, progressive degradation
- Partial authentication support when appropriate

### 🛡️ **Security & Compliance**
- Critical token protection (fail immediately if critical tokens fail)
- Configurable attempt limits per token and overall
- Audit trail of all authentication attempts and failures

### 🔧 **Extensibility**
- Easy to add new brands without touching core code
- Plugin architecture for custom token validators
- Configuration-driven rule definitions

---

## Configuration Files Reference

### Core Configuration Classes
- `BrandAuthConfiguration` - Main interface for brand-specific rules
- `AuthTokenDefinition` - Individual token properties and constraints
- `BrandFailurePolicy` - Failure handling and alternative token strategies
- `TokenValidator` - Custom validation logic per token type

### Implementation Examples
- `PremiumBankAuthConfiguration` - High-security configuration
- `CommunityBankAuthConfiguration` - User-friendly configuration  
- `TechBankAuthConfiguration` - Modern mobile-first configuration

### Service Classes
- `AuthenticationResponseService` - Core token selection algorithm
- `BrandFailurePolicyService` - Alternative token selection logic
- `TokenProcessingService` - Smart re-asking logic implementation
- `BrandAuthConfigurationService` - Brand configuration management

---

## Testing

The system includes comprehensive tests demonstrating the token selection rules:

- `AuthenticationResponseServiceTest` - Core algorithm testing
- `BrandFailurePolicyServiceTest` - Alternative token selection testing
- `FailedTokenDemonstrationTest` - Complete flow demonstrations
- `TokenProcessingServiceTest` - Smart re-asking logic testing

Run tests with:
```bash
mvn test
```

---

## Conclusion

The IVR Authentication system provides a highly sophisticated, configurable approach to token selection that balances security, user experience, and business requirements. The multi-layered architecture allows for complex business rules while maintaining clean separation of concerns and extensibility. 