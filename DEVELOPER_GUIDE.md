# 🛠️ Developer Guide: Bank Authentication Framework

## 🎯 Quick Start Overview

This framework provides **brand-aware, stateful authentication** for IVR systems. Think of it as a configurable authentication engine where each bank brand can have:

- **Custom authentication tokens** (SSN, PIN, Biometrics, etc.)
- **Brand-specific business rules** (who gets asked what, when)
- **DNIS-based routing** (different phone numbers = different rules)
- **Trust-level integration** (high/low trust customers get different flows)

## 🏗️ Core Architecture (5-Minute Overview)

### The Authentication Flow
```
Customer calls → DNIS routing → Brand config → Token selection → Validation → Success/Failure
```

### Key Components
1. **Brand Configuration**: Defines what tokens your brand supports
2. **Token Validators**: How to validate each token type
3. **Business Rules**: Logic for which tokens to ask when
4. **DNIS Config**: Phone-number-based routing rules

## 🚀 Adding a New Brand: Step-by-Step

### Step 1: Define Your Brand Strategy

Before coding, define your bank's authentication strategy:

```yaml
Brand: DIGITAL_BANK
Philosophy: "Mobile-first, biometric-preferred authentication"

Token Priority:
  1. FACE_ID (Priority: 200) - Modern biometric
  2. MOBILE_PIN (Priority: 150) - 6-digit mobile PIN  
  3. SSN_LAST_4 (Priority: 100) - Fallback traditional

Business Rules:
  - Customers under 30: Prefer FACE_ID
  - High-value accounts: Require 2-factor
  - Low trust: Force SSN_LAST_4 + additional token

Security:
  - Max 3 overall attempts
  - No retries for biometric failures
```

### Step 2: Create Brand Configuration

Create your brand configuration class:

```java
// src/main/java/com/bank/ivr/auth/config/DigitalBankAuthConfiguration.java
@Component
public class DigitalBankAuthConfiguration implements BrandAuthConfiguration {
    
    @Override
    public String getBrandCode() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            // Modern biometric - highest priority
            AuthTokenDefinition.builder()
                .name("FACE_ID")
                .description("Face ID Biometric")
                .priority(200)
                .maxAttempts(1)  // No retries for biometrics
                .inputFormatRegex("^[A-F0-9]{64}$")  // Hex hash
                .build(),
                
            // Mobile PIN - second choice
            AuthTokenDefinition.builder()
                .name("MOBILE_PIN")
                .description("6-Digit Mobile PIN")
                .priority(150)
                .maxAttempts(3)
                .inputFormatRegex("^\\d{6}$")
                .build(),
                
            // Traditional fallback
            AuthTokenDefinition.builder()
                .name("SSN_LAST_4")
                .description("Last 4 digits of SSN"
                .priority(100)
                .maxAttempts(2)
                .inputFormatRegex("^\\d{4}$")
                .build()
        );
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 3;  // Strict limit for digital bank
    }
    
    @Override
    public boolean isPartialAuthenticationAllowed() {
        return false;  // Require full authentication
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Digital Bank. Please authenticate using your preferred method.");
        messages.put("failure", "Authentication failed. Please visit our mobile app or call customer service.");
        messages.put("no_methods", "No authentication methods available. Please update your profile in our mobile app.");
        return messages;
    }
}
```

### Step 3: Implement Token Validators

Create validators for your custom tokens:

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
            // Your biometric validation logic
            String storedFaceHash = profile.getFaceIdHash();
            if (storedFaceHash == null) {
                logger.warn("No Face ID hash stored for customer: {}", customerId);
                return false;
            }
            
            // Compare biometric hashes (implement your comparison logic)
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
        return storedPinHash != null && 
               BCrypt.checkpw(providedPin, storedPinHash);
    }
}
```

### Step 4: Create Business Rules (Optional but Powerful)

Add intelligent token selection logic:

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
        
        // High-value customers get mobile PIN first for security
        if (customerProfile.getAccountBalance() > 50000) {
            if (context.getEligibleTokens().contains("MOBILE_PIN") && 
                context.canReAskToken("MOBILE_PIN")) {
                return "MOBILE_PIN";
            }
        }
        
        return null; // Let default logic handle
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

// src/main/java/com/bank/ivr/auth/rule/DigitalBankTrustRule.java
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
        return 250; // Higher priority than age rule
    }
    
    @Override
    public String getRuleName() {
        return "DIGITAL_BANK_TRUST_BASED_SELECTION";
    }
}
```

### Step 5: Add Post-Validation Security Rules

Add extra security checks after successful authentication:

```java
// src/main/java/com/bank/ivr/auth/rule/DigitalBankSecurityRule.java
@Component
public class DigitalBankSecurityRule implements PostValidationRule {
    
    @Override
    public PostValidationResult evaluate(CustomerProfile customerProfile, String brand,
                                       List<String> authenticatedTokens, 
                                       TrustLevelInfo trustLevelInfo) {
        
        // High-value accounts need 2-factor authentication
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

### Step 6: Update Customer Profile Model

Add fields for your new tokens to the CustomerProfile:

```java
// Add to CustomerProfile.java
public class CustomerProfile {
    // ... existing fields ...
    
    private String faceIdHash;
    private String mobilePinHash;
    private Integer age;
    private LocalDateTime lastPasswordChange;
    
    // Add getters and setters
    public String getFaceIdHash() { return faceIdHash; }
    public void setFaceIdHash(String faceIdHash) { this.faceIdHash = faceIdHash; }
    
    public String getMobilePinHash() { return mobilePinHash; }
    public void setMobilePinHash(String mobilePinHash) { this.mobilePinHash = mobilePinHash; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public LocalDateTime getLastPasswordChange() { return lastPasswordChange; }
    public void setLastPasswordChange(LocalDateTime lastPasswordChange) { 
        this.lastPasswordChange = lastPasswordChange; 
    }
}
```

### Step 7: Add Test Data

Add test customers for your brand:

```json
// Add to sample-customer-data.json
{
  "customerId": "DIGITAL001",
  "phoneNumber": "+1555000001",
  "accountNumber": "DIGI001",
  "ssn": "555000001",
  "ssnHash": "$2a$10$encrypted_ssn_hash",
  "faceIdHash": "A1B2C3D4E5F6789...64_char_hex_hash",
  "mobilePinHash": "$2a$10$encrypted_mobile_pin_hash",
  "age": 25,
  "lastPasswordChange": "2024-01-15T10:30:00",
  "accountBalance": 75000,
  "accountStatus": "ACTIVE"
}
```

### Step 8: Configure DNIS (Optional)

Add phone number routing for your brand:

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
  "maxAuthenticationAttempts": 3,
  "sessionTimeoutMinutes": 5
}
```

## 🧪 Testing Your Implementation

### Unit Tests for Your Brand

```java
// src/test/java/com/bank/ivr/auth/config/DigitalBankAuthConfigurationTest.java
@ExtendWith(MockitoExtension.class)
class DigitalBankAuthConfigurationTest {
    
    private DigitalBankAuthConfiguration config;
    
    @BeforeEach
    void setUp() {
        config = new DigitalBankAuthConfiguration();
    }
    
    @Test
    void shouldReturnCorrectBrandCode() {
        assertEquals("DIGITAL_BANK", config.getBrandCode());
    }
    
    @Test
    void shouldHaveFaceIdAsHighestPriority() {
        List<AuthTokenDefinition> tokens = config.getTokenDefinitions();
        
        AuthTokenDefinition faceId = tokens.stream()
            .filter(t -> "FACE_ID".equals(t.getName()))
            .findFirst()
            .orElseThrow();
            
        assertEquals(200, faceId.getPriority());
        assertEquals(1, faceId.getMaxAttempts()); // No retries for biometrics
    }
}

// Integration test
@Test
void shouldAuthenticateDigitalBankCustomerWithFaceId() {
    // Given
    AuthenticationRequest request = new AuthenticationRequest(
        "session-123",
        new CustomerIdentifier("PHONE_NUMBER", "+1555000001"),
        null, // new attempt
        null, // no tokens yet
        "DIGITAL_BANK",
        new TrustLevelInfo("GREEN", "SINGLE_MATCH", 1)
    );
    
    // When - Start authentication
    AuthenticationResponse response = orchestrator.authenticateCustomer(request);
    
    // Then - Should ask for Face ID first
    assertEquals(AuthStatus.PENDING_PRIMARY_TOKEN, response.getStatus());
    assertEquals("FACE_ID", response.getPrimaryTokenToAsk().getName());
    
    // When - Provide Face ID
    AuthenticationRequest continueRequest = new AuthenticationRequest(
        "session-123",
        new CustomerIdentifier("PHONE_NUMBER", "+1555000001"),
        response.getAttemptId(),
        Arrays.asList(new ProvidedToken("FACE_ID", "A1B2C3D4E5F6789...")),
        "DIGITAL_BANK",
        null
    );
    
    AuthenticationResponse finalResponse = orchestrator.authenticateCustomer(continueRequest);
    
    // Then - Should succeed
    assertEquals(AuthStatus.AUTHENTICATED, finalResponse.getStatus());
    assertTrue(finalResponse.getAuthenticatedTokens().contains("FACE_ID"));
}
```

## 🔧 Advanced Features

### Eligibility Rules

Control which tokens are available to specific customers:

```java
@Component
public class DigitalBankBiometricEligibilityRule implements EligibilityRule {
    
    @Override
    public boolean isEligible(CustomerProfile customerProfile, String brand) {
        // Only customers with stored biometric data can use Face ID
        return customerProfile.getFaceIdHash() != null &&
               customerProfile.getAge() != null && 
               customerProfile.getAge() >= 18; // Legal requirement
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

### Trust-Level Integration

Leverage external trust assessment:

```java
@Component
public class DigitalBankTrustBasedRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        if (trustInfo == null) return null;
        
        switch (trustInfo.getTrustLevel()) {
            case "GREEN":
                // High trust - allow modern authentication
                if (context.getEligibleTokens().contains("FACE_ID")) {
                    return "FACE_ID";
                }
                break;
                
            case "RED": 
                // Low trust - require traditional verification
                if (context.getEligibleTokens().contains("SSN_LAST_4")) {
                    return "SSN_LAST_4";
                }
                break;
        }
        
        return null;
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public int getPriority() {
        return 300; // Very high priority
    }
}
```

## 📊 Rule Execution Flow

Understanding how rules work together:

```
1. ELIGIBILITY RULES (Run Once at Session Start)
   ├── Check if customer has Face ID → ✅ Eligible tokens: [FACE_ID, MOBILE_PIN, SSN_LAST_4]
   └── Store eligible tokens in context

2. TOKEN SELECTION RULES (Run Every Time We Need a Token)
   ├── DigitalBankTrustBasedRule (Priority: 300) → "Low trust, return SSN_LAST_4"
   ├── DigitalBankAgeBasedRule (Priority: 200) → ❌ Skipped (higher priority rule already returned)
   └── Default fallback → ❌ Not reached

3. POST-VALIDATION RULES (Run After Each Successful Token)
   ├── DigitalBankSecurityRule → "High value account, need 2-factor"
   └── Continue authentication with additional token requirement
```

## 🚨 Common Pitfalls and Best Practices

### ❌ Don't Do This:
```java
// DON'T: Hard-code brand logic in validators
if (brand.equals("DIGITAL_BANK") && someCondition) {
    // This makes validators brittle
}

// DON'T: Ignore rule priorities
public int getPriority() {
    return 1; // Too low - will never execute
}

// DON'T: Forget brand specificity
public String getBrand() {
    return "DEFAULT"; // Will apply to ALL brands
}
```

### ✅ Do This:
```java
// DO: Keep validators simple and focused
@Override
public boolean validate(String customerId, String providedValue, CustomerProfile profile) {
    return simpleValidationLogic(providedValue, profile.getStoredValue());
}

// DO: Use meaningful priorities
public int getPriority() {
    return 200; // Clear intent: higher than default (100), lower than critical (300)
}

// DO: Be explicit about brand targeting
public String getBrand() {
    return "DIGITAL_BANK"; // Only applies to this brand
}
```

## 🔄 Development Workflow

1. **Design**: Define your authentication strategy
2. **Configure**: Create brand configuration
3. **Validate**: Implement token validators  
4. **Rules**: Add business logic (optional)
5. **Test**: Write comprehensive tests
6. **Deploy**: Add to production configuration

## 📈 Monitoring Your Brand

The framework provides built-in monitoring:

```java
// Authentication attempts by brand
logger.info("Brand: {}, Token: {}, Success: {}", brand, tokenName, success);

// Rule execution tracking  
logger.debug("Rule {} executed for brand {} with priority {}", ruleName, brand, priority);

// Performance metrics
logger.debug("Authentication completed in {}ms for brand {}", duration, brand);
```

## 🎯 Quick Reference

### Essential Files for New Brand:
- `BrandAuthConfiguration` - Token definitions and limits
- `TokenValidator` implementations - Validation logic
- `TokenSelectionRule` implementations - Business logic (optional)
- `PostValidationRule` implementations - Security rules (optional)
- Test data in JSON files
- Comprehensive unit tests

### Key Concepts:
- **Brand Code**: Unique identifier for your bank
- **Token Priority**: Higher numbers = asked first
- **Rule Priority**: Higher numbers = executed first
- **Eligibility**: Who can use which tokens
- **Smart Re-asking**: Won't re-ask failed tokens
- **DNIS**: Phone-number-based routing

Ready to build your brand's authentication experience! 🚀 