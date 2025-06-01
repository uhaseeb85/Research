# 🚀 Quick Reference: Adding a New Brand

## ⚡ 5-Minute Checklist

### 1. Brand Configuration (Required)
```java
@Component
public class YourBankAuthConfiguration implements BrandAuthConfiguration {
    public String getBrandCode() { return "YOUR_BANK"; }
    public List<AuthTokenDefinition> getTokenDefinitions() { /* Define tokens */ }
    public int getMaxOverallAttempts() { return 5; }
}
```

### 2. Token Validators (Required for Custom Tokens)
```java
@Component
public class YourTokenValidator implements TokenValidator {
    public String getTokenName() { return "YOUR_TOKEN"; }
    public String getBrand() { return "YOUR_BANK"; }
    public boolean validate(String customerId, String value, CustomerProfile profile) { /* Logic */ }
}
```

### 3. Business Rules (Optional)
```java
@Component
public class YourBankRule implements TokenSelectionRule {
    public String getBrand() { return "YOUR_BANK"; }
    public int getPriority() { return 200; }
    public String determineNextToken(AuthenticationContext context, CustomerProfile profile) { /* Logic */ }
}
```

### 4. Test Data (Required)
```json
// Add to sample-customer-data.json
{
  "customerId": "YOUR001",
  "phoneNumber": "+1555000001", 
  "accountNumber": "ACC001",
  // ... add fields for your custom tokens
}
```

## 🎯 Key Concepts

| Concept | Description | Example |
|---------|-------------|---------|
| **Brand Code** | Unique identifier | `"DIGITAL_BANK"` |
| **Token Priority** | Higher = asked first | `200` (high), `100` (normal), `50` (low) |
| **Rule Priority** | Higher = executed first | `300` (critical), `200` (high), `100` (normal) |
| **Eligibility** | Who can use which tokens | Customer has biometric data → can use Face ID |
| **Smart Re-asking** | Won't re-ask failed tokens | User provided wrong PIN → won't ask PIN again |

## 🔄 Token Selection Flow

```
1. New Request → Check if new attempt (attemptId == null)
2. If new → Run ELIGIBILITY rules → Store eligible tokens
3. Need token → Run TOKEN SELECTION rules (by priority)
4. First rule returns token → Use it (skip remaining rules)
5. Validate token → If success → Run POST-VALIDATION rules
6. Continue until authenticated or failed
```

## 🎨 Common Patterns

### High-Security Bank
```java
// Strict 2-factor authentication
public int getMaxOverallAttempts() { return 2; }
public boolean isPartialAuthenticationAllowed() { return false; }

// Biometrics + Traditional
AuthTokenDefinition.builder()
    .name("FINGERPRINT").priority(200).maxAttempts(1)
    .name("SSN_FULL").priority(150).maxAttempts(2)
```

### Mobile-First Bank  
```java
// Modern, user-friendly
public int getMaxOverallAttempts() { return 5; }

// Mobile-first tokens
AuthTokenDefinition.builder()
    .name("FACE_ID").priority(200).maxAttempts(1)
    .name("MOBILE_PIN").priority(150).maxAttempts(3)
    .name("SMS_CODE").priority(100).maxAttempts(2)
```

### Community Bank
```java
// Simple, traditional
AuthTokenDefinition.builder()
    .name("SSN").priority(100).maxAttempts(3)
    .name("DATE_OF_BIRTH").priority(95).maxAttempts(2)
    .name("MOTHER_MAIDEN_NAME").priority(90).maxAttempts(2)
```

## 🧪 Testing Checklist

- [ ] Brand configuration returns correct code
- [ ] Token definitions have correct priorities  
- [ ] Validators work for valid/invalid inputs
- [ ] Rules execute in correct order
- [ ] Test customers have required data fields
- [ ] Integration test covers full auth flow
- [ ] Error cases are handled gracefully

## 🚨 Common Mistakes

| ❌ Don't | ✅ Do |
|----------|-------|
| `getBrand() { return "DEFAULT"; }` | `getBrand() { return "YOUR_BANK"; }` |
| `getPriority() { return 1; }` | `getPriority() { return 200; }` |
| Hard-code brand logic in validators | Keep validators simple and focused |
| Forget to add test data | Add comprehensive test customers |
| Skip integration tests | Test full authentication flow |

## 📁 File Locations

```
src/main/java/com/bank/ivr/auth/
├── config/YourBankAuthConfiguration.java
├── validator/YourTokenValidator.java  
├── rule/YourBankRule.java (optional)
└── rule/YourBankPostValidationRule.java (optional)

src/main/resources/
├── sample-customer-data.json (add test customers)
└── dnis-configurations.json (add phone numbers)

src/test/java/com/bank/ivr/auth/
├── config/YourBankAuthConfigurationTest.java
├── validator/YourTokenValidatorTest.java
└── integration/YourBankIntegrationTest.java
```

## 🔧 Development Commands

```bash
# Build and test
mvn clean compile
mvn test

# Run specific test
mvn test -Dtest=YourBankAuthConfigurationTest

# Start application  
mvn spring-boot:run

# Test your API
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test","customerIdentifier":{"type":"PHONE_NUMBER","value":"+1555000001"},"brand":"YOUR_BANK"}'
```

## 🎯 API Testing

### Start Authentication
```json
POST /api/v1/auth/customer
{
  "sessionId": "session-123",
  "customerIdentifier": {"type": "PHONE_NUMBER", "value": "+1555000001"},
  "brand": "YOUR_BANK",
  "trustLevelInfo": {"trustLevel": "GREEN", "phoneMatchStatus": "SINGLE_MATCH"}
}
```

### Continue with Token
```json
POST /api/v1/auth/customer  
{
  "sessionId": "session-123",
  "customerIdentifier": {"type": "PHONE_NUMBER", "value": "+1555000001"},
  "attemptId": "from-previous-response",
  "providedTokens": [{"tokenName": "YOUR_TOKEN", "tokenValue": "test123"}],
  "brand": "YOUR_BANK"
}
```

## 🎨 Priority Guidelines

| Priority Range | Usage | Examples |
|----------------|-------|----------|
| **300+** | Critical overrides | Trust-based routing, fraud detection |
| **200-299** | Brand-specific business logic | Age-based preferences, VIP handling |  
| **100-199** | Standard token priorities | Normal authentication flow |
| **50-99** | Fallback logic | Default rules, error handling |

Ready to build! 🚀 