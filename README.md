# 🏦 Authentication Framework - Developer Guide

## 🎯 Overview

**Java 1.8 Compatible** | **Spring Boot 2.7.18** | **In-Memory Storage**

## 🏗️ Framework Architecture

### Core Components
1. **Brand Configuration** - Defines tokens, priorities, and policies for your bank
2. **Token Validators** - Implements validation logic for each authentication method
3. **Business Rules** - Controls authentication flow based on customer context
4. **DNIS Configuration** - Phone-number-based routing and security policies
5. **Session Management** - Stateful authentication context with automatic cleanup

### Authentication Flow
```
Customer calls → DNIS lookup → Brand config → Eligibility check → Token selection → Validation → Post-validation rules → Success/Continue/Fail
```

### Rule Execution Order
```
1. ELIGIBILITY RULES (Once per session)
   ├── Determine available tokens based on customer profile
   └── Store in authentication context

2. TOKEN SELECTION RULES (Every token request)
   ├── Execute by priority (highest first)
   ├── First rule returning a token wins
   └── Fallback to priority-based selection

3. POST-VALIDATION RULES (After each successful token)
   ├── Security and risk assessment
   └── May require additional authentication
```

## 📞 DNIS Configuration (Phone Number Routing)

### How DNIS Works
1. **DNIS Retrieval**: Retrieved from session context using `sessionId`
2. **Configuration Lookup**: System finds specific DNIS config or uses `"DEFAULT"`
3. **Rule Application**: DNIS settings override brand defaults

### DNIS Configuration Example
```json
// Add to dnis-configurations.json
{
  "dnis": "18005551111",
  "description": "Digital Bank mobile support line",
  "allowSsnAuthentication": true,
  "allowFaceIdAuthentication": true,
  "allowMobilePinAuthentication": true,
  "requireMultiFactorAuth": false,
  "allowTrustLevelBypass": true,
  "enableStrictValidation": false,
  "maxAuthenticationAttempts": 3,
  "sessionTimeoutMinutes": 5
}
```

### Default DNIS Behavior
- **If no DNIS found**: Uses `"DEFAULT"` configuration automatically
- **If DNIS not configured**: Falls back to `"DEFAULT"` configuration  
- **Default settings**: Permissive (all methods allowed, standard security)

## 🔧 Development Commands

```bash
# Build and run tests
mvn clean install

# Run specific brand tests
mvn test -Dtest=DigitalBankIntegrationTest

# Start application
mvn spring-boot:run

# Test your brand API
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session",
    "customerIdentifier": {
      "type": "PHONE_NUMBER",
      "value": "+1555000001"
    },
    "brand": "DIGITAL_BANK",
    "trustLevelInfo": {
      "trustLevel": "GREEN",
      "phoneMatchStatus": "SINGLE_MATCH",
      "matchedSsnCount": 1
    }
  }'
```

## 🚀 Quick Start - Add Your Bank in 5 Steps

### Step 1: Define Your Authentication Strategy

```yaml
Brand: DIGITAL_BANK
Philosophy: "Mobile-first, biometric-preferred authentication"

Token Priority:
  1. FACE_ID (Priority: 200) - Modern biometric, 1 attempt only
  2. MOBILE_PIN (Priority: 150) - 6-digit mobile PIN, 3 attempts  
  3. SSN_LAST_4 (Priority: 100) - Traditional fallback, 2 attempts

Business Rules:
  - Customers under 30: Prefer FACE_ID
  - High-value accounts ($100k+): Require 2-factor authentication
  - Low trust level: Force SSN_LAST_4 + additional verification

Security:
  - Max 3 overall attempts per session
  - No retries for biometric failures (security policy)
  - Enhanced verification for accounts with recent changes
```

### Step 2: Create Brand Configuration

```java
// src/main/java/com/bank/ivr/auth/config/DigitalBankAuthConfiguration.java
@Component
public class DigitalBankAuthConfiguration implements BrandAuthConfiguration {
    
    // Static collections for performance optimization
    private static final List<AuthTokenDefinition> TOKEN_DEFINITIONS;
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES;
    private static final List<String> APPLICABLE_ELIGIBILITY_RULES;
    private static final List<String> APPLICABLE_POST_VALIDATION_RULES;
    private static final Map<String, Integer> RULE_PRIORITIES;
    private static final Map<String, String> BRAND_MESSAGES;
    
    static {
        // Initialize token definitions with brand-specific priorities and attempt limits
        TOKEN_DEFINITIONS = Arrays.asList(
            // Modern biometric - highest priority, no retries
            AuthTokenDefinition.builder()
                .name("FACE_ID")
                .description("Face ID Biometric Authentication")
                .priority(200)
                .maxAttempts(1)  // Security: No retries for biometrics
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
        
        // Configure which rules apply to this brand
        APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
            "TRUST_BASED_SECURITY_RULE",        // Security first (Priority: 300)
            "HIGH_VALUE_CUSTOMER_RULE",         // High-value customer logic (Priority: 250)
            "DIGITAL_BANK_AGE_RULE",           // Brand-specific age-based preference (Priority: 200)
            "DIGITAL_BANK_BIOMETRIC_PREFERENCE" // Brand-specific biometric preference (Priority: 180)
        );
        
        APPLICABLE_ELIGIBILITY_RULES = Arrays.asList(
            "DIGITAL_BANK_BIOMETRIC_ELIGIBILITY_RULE",
            "STANDARD_SSN_ELIGIBILITY_RULE"
        );
        
        APPLICABLE_POST_VALIDATION_RULES = Arrays.asList(
            "DIGITAL_BANK_SECURITY_RULE",
            "HIGH_VALUE_ADDITIONAL_AUTH_RULE"
        );
        
        // Configure rule priorities (overrides default rule priorities)
        Map<String, Integer> priorities = new HashMap<>();
        priorities.put("TRUST_BASED_SECURITY_RULE", 300);        // Security always first
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 250);         // High-value customers second
        priorities.put("DIGITAL_BANK_AGE_RULE", 200);           // Age-based preferences third
        priorities.put("DIGITAL_BANK_BIOMETRIC_PREFERENCE", 180); // Biometric preference fourth
        priorities.put("DIGITAL_BANK_SECURITY_RULE", 150);       // Post-validation security
        priorities.put("HIGH_VALUE_ADDITIONAL_AUTH_RULE", 100);  // Additional auth requirements
        RULE_PRIORITIES = priorities;
        
        // Brand-specific messages
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Digital Bank. Please authenticate using your preferred method.");
        messages.put("failure", "Authentication failed. Please visit our mobile app or call customer service.");
        messages.put("no_methods", "No authentication methods available. Please update your profile in our mobile app.");
        messages.put("customer_not_found", "Customer profile not found. Please verify your information.");
        messages.put("session_expired", "Your session has expired. Please call again to restart authentication.");
        BRAND_MESSAGES = messages;
    }
    
    @Override
    public String getBrandCode() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return TOKEN_DEFINITIONS;
    }
    
    // ============= RULE CONFIGURATION METHODS =============
    
    @Override
    public List<String> getApplicableTokenSelectionRules() {
        return APPLICABLE_TOKEN_SELECTION_RULES;
    }
    
    @Override
    public List<String> getApplicableEligibilityRules() {
        return APPLICABLE_ELIGIBILITY_RULES;
    }
    
    @Override
    public List<String> getApplicablePostValidationRules() {
        return APPLICABLE_POST_VALIDATION_RULES;
    }
    
    @Override
    public Map<String, Integer> getRulePriorities() {
        return RULE_PRIORITIES;
    }
    
    @Override
    public boolean isRuleEnabled(String ruleName) {
        // All configured rules are enabled by default
        return APPLICABLE_TOKEN_SELECTION_RULES.contains(ruleName) ||
               APPLICABLE_ELIGIBILITY_RULES.contains(ruleName) ||
               APPLICABLE_POST_VALIDATION_RULES.contains(ruleName);
    }
    
    // ============= BRAND SETTINGS =============
    
    @Override
    public int getMaxOverallAttempts() {
        return 3;  // Strict security for digital bank
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
    public boolean isPartialAuthenticationAllowed() {
        return false;  // Require full authentication always
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        return BRAND_MESSAGES;
    }
}
```

### Step 3: Implement Token Validators

Create validators for each custom token:

```java
// src/main/java/com/bank/ivr/auth/validator/FaceIdValidator.java
@Component
public class FaceIdValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceIdValidator.class);
    
    @Override
    public String getTokenName() {
        return "FACE_ID";
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK";  // Brand-specific validator
    }
    
    @Override
    public boolean validate(String customerId, String providedHash, CustomerProfile profile) {
        try {
            String storedFaceHash = profile.getFaceIdHash();
            if (storedFaceHash == null) {
                logger.warn("No Face ID hash stored for customer: {}", customerId);
                return false;
            }
            
            // Implement your biometric comparison logic
            boolean matches = BiometricUtil.compareFaceHashes(providedHash, storedFaceHash);
            
            logger.debug("Face ID validation for customer {}: {}", 
                        customerId, matches ? "SUCCESS" : "FAILED");
            return matches;
            
        } catch (Exception e) {
            logger.error("Face ID validation error for customer: {}", customerId, e);
            return false;
        }
    }
}

// src/main/java/com/bank/ivr/auth/validator/MobilePinValidator.java
@Component
public class MobilePinValidator implements TokenValidator {
    
    @Override
    public String getTokenName() {
        return "MOBILE_PIN";
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public boolean validate(String customerId, String providedPin, CustomerProfile profile) {
        // Use BCrypt for secure PIN comparison
        String storedPinHash = profile.getMobilePinHash();
        return storedPinHash != null && BCrypt.checkpw(providedPin, storedPinHash);
    }
}
```

### Step 4: Add Intelligent Business Rules (Optional but Powerful)

#### Token Selection Rules - Control Which Token to Ask When

```java
// src/main/java/com/bank/ivr/auth/rule/DigitalBankAgeBasedRule.java
@Component
public class DigitalBankAgeBasedRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        // Young customers prefer biometrics
        if (customerProfile.getAge() != null && customerProfile.getAge() < 30) {
            if (context.getEligibleTokens().contains("FACE_ID") && 
                context.canReAskToken("FACE_ID")) {
                return "FACE_ID";
            }
        }
        
        // High-value customers get mobile PIN for enhanced security
        if (customerProfile.getAccountBalance() > 50000) {
            if (context.getEligibleTokens().contains("MOBILE_PIN") && 
                context.canReAskToken("MOBILE_PIN")) {
                return "MOBILE_PIN";
            }
        }
        
        return null; // Let default priority-based logic handle
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public int getPriority() {
        return 200; // High priority custom rule
    }
    
    @Override
    public String getRuleName() {
        return "DIGITAL_BANK_AGE_BASED_SELECTION";
    }
}

// Trust-based token selection
@Component
public class DigitalBankTrustRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        if (trustInfo != null && "RED".equals(trustInfo.getTrustLevel())) {
            // Low trust - force traditional authentication
            if (context.getEligibleTokens().contains("SSN_LAST_4") && 
                context.canReAskToken("SSN_LAST_4")) {
                return "SSN_LAST_4";
            }
        }
        
        return null;
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public int getPriority() {
        return 250; // Higher priority than age rule - security first
    }
    
    @Override
    public String getRuleName() {
        return "DIGITAL_BANK_TRUST_BASED_SELECTION";
    }
}
```

#### Post-Validation Rules - Additional Security After Successful Authentication

```java
// src/main/java/com/bank/ivr/auth/rule/DigitalBankSecurityRule.java
@Component
public class DigitalBankSecurityRule implements PostValidationRule {
    
    @Override
    public PostValidationResult evaluate(CustomerProfile customerProfile, String brand,
                                       List<String> authenticatedTokens, 
                                       TrustLevelInfo trustLevelInfo) {
        
        // High-value accounts need multi-factor authentication
        if (customerProfile.getAccountBalance() > 100000 && 
            authenticatedTokens.size() < 2) {
            
            return PostValidationResult.builder()
                .requireAdditionalAuth(true)
                .requiredTokens(Arrays.asList("MOBILE_PIN", "SSN_LAST_4"))
                .riskLevel("HIGH")
                .reason("High-value account requires multi-factor authentication")
                .build();
        }
        
        // Recent password change requires additional verification
        if (customerProfile.getLastPasswordChange() != null && 
            customerProfile.getLastPasswordChange().isAfter(LocalDateTime.now().minusDays(7))) {
            
            return PostValidationResult.builder()
                .requireAdditionalAuth(true)
                .requiredTokens(Arrays.asList("SSN_LAST_4"))
                .riskLevel("MEDIUM")
                .reason("Recent password change detected - additional verification required")
                .build();
        }
        
        return PostValidationResult.noAdditionalAuth();
    }
    
    @Override
    public String getRuleName() {
        return "DIGITAL_BANK_SECURITY_RULE";
    }
    
    @Override
    public int getPriority() {
        return 150;
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK";
    }
}
```

#### Eligibility Rules - Control Which Tokens Are Available to Customers

```java
// src/main/java/com/bank/ivr/auth/rule/DigitalBankBiometricEligibilityRule.java
@Component
public class DigitalBankBiometricEligibilityRule implements EligibilityRule {
    
    @Override
    public boolean isEligible(CustomerProfile customerProfile, String brand) {
        // Only customers with stored biometric data and legal age can use Face ID
        return customerProfile.getFaceIdHash() != null &&
               customerProfile.getAge() != null && 
               customerProfile.getAge() >= 18 && // Legal requirement
               "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getTokenName() {
        return "FACE_ID";
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
}
```

## 🎯 Brand-Configured Rules System

### Overview

The framework now uses a **brand-configured rules approach** where rules are:
- **Brand-agnostic** by design (no hardcoded brand logic in rules)
- **Configured per brand** in the brand configuration  
- **Dynamically loaded** with brand-specific priorities
- **Reusable across brands** with different priorities and configurations

### How Brand-Configured Rules Work

Instead of rules having `getBrand()` methods, brands configure which rules apply:

```java
@Component
public class DigitalBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public List<String> getApplicableTokenSelectionRules() {
        return Arrays.asList(
            "TRUST_BASED_SECURITY_RULE",        // Shared rule
            "HIGH_VALUE_CUSTOMER_RULE",         // Shared rule  
            "DIGITAL_BANK_AGE_RULE",           // Brand-specific rule
            "DIGITAL_BANK_BIOMETRIC_PREFERENCE" // Brand-specific rule
        );
    }
    
    @Override
    public Map<String, Integer> getRulePriorities() {
        Map<String, Integer> priorities = new HashMap<>();
        // Same rule, different priorities per brand
        priorities.put("TRUST_BASED_SECURITY_RULE", 300);
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 250);
        priorities.put("DIGITAL_BANK_AGE_RULE", 200);
        return priorities;
    }
}
```

### Creating Brand-Agnostic Rules

Rules are now **completely brand-agnostic** with proper Spring bean names:

```java
@Component("TRUST_BASED_SECURITY_RULE")  // Spring bean name
public class TrustBasedSecurityRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile profile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        if (trustInfo != null && "RED".equals(trustInfo.getTrustLevel())) {
            // Low trust - force full SSN authentication
            if (context.getEligibleTokens().contains("SSN_FULL") && 
                context.canReAskToken("SSN_FULL")) {
                return "SSN_FULL";
            }
        }
        
        return null; // Let other rules handle
    }
    
    @Override
    public int getPriority() {
        return 250; // Default priority, overridden by brand config
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Always DEFAULT for brand-agnostic rules
    }
    
    @Override
    public String getRuleName() {
        return "TRUST_BASED_SECURITY_RULE"; // Match Spring bean name
    }
}
```

### Multi-Brand Rule Configuration Examples

#### Example 1: Same Rule, Different Priorities

```java
// Digital Bank Configuration with static collections
@Component
public class DigitalBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final Map<String, Integer> RULE_PRIORITIES;
    static {
        Map<String, Integer> priorities = new HashMap<>();
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 250); // Higher priority
        RULE_PRIORITIES = priorities;
    }
    
    @Override
    public Map<String, Integer> getRulePriorities() {
        return RULE_PRIORITIES;
    }
}

// Community Bank Configuration with static collections  
@Component
public class CommunityBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final Map<String, Integer> RULE_PRIORITIES;
    static {
        Map<String, Integer> priorities = new HashMap<>();
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 150); // Lower priority
        RULE_PRIORITIES = priorities;
    }
    
    @Override
    public Map<String, Integer> getRulePriorities() {
        return RULE_PRIORITIES;
    }
}
```

#### Example 2: Shared Rules Across Brands

```java
// Brand-agnostic rule that both banks can use
@Component("TRUST_BASED_SECURITY_RULE")
public class TrustBasedSecurityRule implements TokenSelectionRule {
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Always DEFAULT for shared rules
    }
    
    @Override
    public String getRuleName() {
        return "TRUST_BASED_SECURITY_RULE"; // Match Spring bean name
    }
    
    // Implementation details...
}

// Digital Bank enables it with high priority
@Component
public class DigitalBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
        "TRUST_BASED_SECURITY_RULE" // Configured with priority 300
    );
    
    @Override
    public List<String> getApplicableTokenSelectionRules() {
        return APPLICABLE_TOKEN_SELECTION_RULES;
    }
}

// Community Bank enables it with lower priority  
@Component
public class CommunityBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
        "TRUST_BASED_SECURITY_RULE" // Configured with priority 200
    );
    
    @Override
    public List<String> getApplicableTokenSelectionRules() {
        return APPLICABLE_TOKEN_SELECTION_RULES;
    }
}
```

#### Example 3: Brand-Specific Rules

```java
// Rule that only makes sense for Premium Bank
@Component("PREMIUM_BANK_VOICE_BIOMETRIC_RULE")
public class VoiceBiometricRule implements TokenSelectionRule {
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Still DEFAULT, but only Premium Bank configures it
    }
    
    @Override
    public String getRuleName() {
        return "PREMIUM_BANK_VOICE_BIOMETRIC_RULE";
    }
    
    // Voice biometric specific implementation...
}

// Only Premium Bank configures this rule
@Component  
public class PremiumBankAuthConfiguration implements BrandAuthConfiguration {
    
    private static final List<AuthTokenDefinition> TOKEN_DEFINITIONS = Arrays.asList(
        AuthTokenDefinition.builder()
                .name("DEBIT_CARD_PIN")
                .priority(100)
                .maxAttempts(3) // Premium Bank's PIN policy
                .build(),
                
        AuthTokenDefinition.builder()
                .name("SSN")
                .priority(95)
                .maxAttempts(2) // Premium Bank's SSN policy (stricter)
                .build()
    );
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return TOKEN_DEFINITIONS;
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
}
```

### Benefits of Brand-Configured Rules

| **Old Approach** | **New Approach** |
|------------------|------------------|
| Rules hardcode brand logic | Rules are completely brand-agnostic |
| Same rule, different brands = duplicate code | Same rule reused across brands |
| Priority fixed in rule code | Priority configured per brand |
| Hard to share rules | Easy to share rules |
| Must create new rule per brand | Configure existing rules per brand |

### Configuration Methods

```java
public interface BrandAuthConfiguration {
    
    // TOKEN SELECTION RULES
    List<String> getApplicableTokenSelectionRules();  // Which rules to use
    
    // ELIGIBILITY RULES  
    List<String> getApplicableEligibilityRules();     // Which rules to use
    
    // POST-VALIDATION RULES
    List<String> getApplicablePostValidationRules();  // Which rules to use
    
    // RULE PRIORITIES
    Map<String, Integer> getRulePriorities();         // Override rule priorities
    
    // RULE STATUS
    boolean isRuleEnabled(String ruleName);           // Check if rule enabled
}
```

### 🚀 Quick Start: Brand-Configured Rules

#### Step 1: Create Brand-Agnostic Rules

```java
@Component("TRUST_BASED_SECURITY_RULE")
public class TrustBasedSecurityRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile profile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        if (trustInfo != null && "RED".equals(trustInfo.getTrustLevel())) {
            // Low trust - force full SSN authentication
            if (context.getEligibleTokens().contains("SSN_FULL") && 
                context.canReAskToken("SSN_FULL")) {
                return "SSN_FULL";
            }
        }
        
        return null; // Let other rules handle
    }
    
    @Override
    public int getPriority() {
        return 250; // Default priority, overridden by brand config
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Always DEFAULT for brand-agnostic rules
    }
    
    @Override
    public String getRuleName() {
        return "TRUST_BASED_SECURITY_RULE"; // Match Spring bean name
    }
}

@Component("HIGH_VALUE_CUSTOMER_RULE")
public class HighValueCustomerRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile profile) {
        if (isHighValueCustomer(profile)) {
            // Prefer mobile PIN for high-value customers
            if (context.getEligibleTokens().contains("MOBILE_PIN") && 
                context.canReAskToken("MOBILE_PIN")) {
                return "MOBILE_PIN";
            }
        }
        return null;
    }
    
    private boolean isHighValueCustomer(CustomerProfile profile) {
        // Employee ID or premium account status indicates high-value customer
        return (profile.getEmployeeId() != null && !profile.getEmployeeId().trim().isEmpty()) ||
               "PREMIUM".equals(profile.getAccountStatus()) || 
               "VIP".equals(profile.getAccountStatus());
    }
    
    @Override
    public int getPriority() {
        return 200; // Default priority, overridden by brand config
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Always DEFAULT for brand-agnostic rules
    }
    
    @Override
    public String getRuleName() {
        return "HIGH_VALUE_CUSTOMER_RULE";
    }
}
```

#### Step 2: Configure Rules Per Brand with Static Collections

```java
@Component
public class DigitalBankAuthConfiguration implements BrandAuthConfiguration {
    
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
    
    @Override
    public List<String> getApplicableTokenSelectionRules() {
        return APPLICABLE_TOKEN_SELECTION_RULES;
    }
    
    @Override
    public Map<String, Integer> getRulePriorities() {
        return RULE_PRIORITIES;
    }
    
    // ... other configuration methods
}

@Component
public class CommunityBankAuthConfiguration implements BrandAuthConfiguration {
    
    // Static rule configurations for performance
    private static final List<String> APPLICABLE_TOKEN_SELECTION_RULES = Arrays.asList(
        "TRUST_BASED_SECURITY_RULE",     // Security first, but lower priority
        "HIGH_VALUE_CUSTOMER_RULE"       // Simple high-value detection
    );
    
    private static final Map<String, Integer> RULE_PRIORITIES;
    static {
        Map<String, Integer> priorities = new HashMap<>();
        priorities.put("TRUST_BASED_SECURITY_RULE", 200);  // Lower priority than digital bank
        priorities.put("HIGH_VALUE_CUSTOMER_RULE", 150);   // Conservative approach
        RULE_PRIORITIES = priorities;
    }
    
    @Override
    public List<String> getApplicableTokenSelectionRules() {
        return APPLICABLE_TOKEN_SELECTION_RULES;
    }
    
    @Override
    public Map<String, Integer> getRulePriorities() {
        return RULE_PRIORITIES;
    }
    
    // ... other configuration methods
}
```

Ignore

#### Step 3: Results

With this configuration:

- **Digital Bank**: Modern biometric-first approach with sophisticated rules
- **Community Bank**: Traditional conservative approach with basic security  
- **Same Rules**: Both banks use shared rules but with different priorities
- **Performance**: Static collections avoid object creation on every call
- **Easy Maintenance**: Update one rule, benefits all configured banks

**Rule Execution for high-value customer:**

| Bank | Rule Execution Order | Selected Token |
|------|---------------------|----------------|
| Digital Bank | 1. TRUST_BASED_SECURITY_RULE (300)<br>2. HIGH_VALUE_CUSTOMER_RULE (250) ✅ | MOBILE_PIN |
| Community Bank | 1. TRUST_BASED_SECURITY_RULE (200)<br>2. HIGH_VALUE_CUSTOMER_RULE (150) | SSN |

## 📡 API Endpoints

### Authentication
```
