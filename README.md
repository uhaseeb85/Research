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
    
    @Override
    public String getBrandCode() {
        return "DIGITAL_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
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
    }
    
    @Override
    public int getMaxOverallAttempts() {
        return 3;  // Strict security for digital bank
    }
    
    @Override
    public boolean isPartialAuthenticationAllowed() {
        return false;  // Require full authentication always
    }
    
    @Override
    public Map<String, String> getBrandMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", "Welcome to Digital Bank. Please authenticate using your preferred method.");
        messages.put("failure", "Authentication failed. Please visit our mobile app or call customer service.");
        messages.put("no_methods", "No authentication methods available. Please update your profile in our mobile app.");
        messages.put("customer_not_found", "Customer profile not found. Please verify your information.");
        messages.put("session_expired", "Your session has expired. Please call again to restart authentication.");
        return messages;
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

## 📡 API Endpoints

### Authentication
```http
POST /api/v1/auth/customer
Content-Type: application/json

{
  "sessionId": "session-123",
  "customerIdentifier": {
    "type": "PHONE_NUMBER",
    "value": "+1234567890"
  },
  "brand": "DIGITAL_BANK",
  "trustLevelInfo": {
    "trustLevel": "GREEN",
    "phoneMatchStatus": "SINGLE_MATCH",
    "matchedSsnCount": 1
  }
}
```

### Brand Information
```http
GET /api/v1/auth/methods/DIGITAL_BANK    # Get authentication methods
GET /api/v1/auth/brands                  # Get all supported brands
```

### DNIS Configuration
```http
GET /api/v1/auth/dnis/18005551111        # Get specific DNIS config
GET /api/v1/auth/dnis                    # Get all DNIS configurations
```

## 🚨 Best Practices & Common Pitfalls

### ✅ Best Practices
```java
// DO: Keep validators simple and focused
@Override
public boolean validate(String customerId, String value, CustomerProfile profile) {
    return BCrypt.checkpw(value, profile.getStoredHash());
}

// DO: Use meaningful rule priorities
public int getPriority() {
    return 200; // Higher than default (100), lower than critical security (300)
}

// DO: Be explicit about brand targeting
public String getBrand() {
    return "DIGITAL_BANK"; // Specific brand only
}

// DO: Handle edge cases gracefully
if (customerProfile.getAge() == null) {
    return null; // Let other rules handle
}
```

### ❌ Common Pitfalls
```java
// DON'T: Hard-code brand logic in validators
if (brand.equals("DIGITAL_BANK") && someCondition) {
    // This creates tight coupling
}

// DON'T: Use low priorities that never execute
public int getPriority() {
    return 1; // Will be overridden by everything
}

// DON'T: Apply rules to all brands unintentionally
public String getBrand() {
    return "DEFAULT"; // Affects ALL brands
}

// DON'T: Ignore null safety
customerProfile.getAge() < 30; // NPE if age is null
```

## 🧪 Testing Strategy

### Test Categories
1. **Unit Tests** - Individual component testing
2. **Integration Tests** - Full authentication flows
3. **Brand-Specific Tests** - Custom token and rule testing
4. **DNIS Tests** - Phone number routing scenarios
5. **Security Tests** - Failed token and attack scenarios

### Essential Test Scenarios
- **Happy path**: Normal authentication flow
- **Multi-factor**: High-value customer scenarios  
- **Trust levels**: RED vs GREEN trust handling
- **Failed tokens**: Invalid input handling
- **DNIS routing**: Different phone number behaviors
- **Rule conflicts**: Multiple rules interaction

## 📊 Monitoring & Observability

The framework provides comprehensive logging:

```java
// Authentication flow tracking
logger.info("Brand: {}, Customer: {}, Token: {}, Result: {}", 
           brand, customerId, tokenName, result);

// Rule execution monitoring
logger.debug("Rule '{}' executed for brand '{}' with priority {}", 
            ruleName, brand, priority);

// Performance metrics
logger.debug("Authentication completed in {}ms for brand '{}'", 
            duration, brand);

// Security events
logger.warn("Multiple failed attempts for customer: {}, brand: {}", 
           customerId, brand);
```