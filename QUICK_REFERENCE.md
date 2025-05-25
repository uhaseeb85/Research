# 🚀 IVR Authentication - Quick Reference

## 🎯 Core Concepts

### TokenValidator Interface
```java
@Component
public class MyValidator implements TokenValidator {
    @Override public String getTokenName() { return "MY_TOKEN"; }
    @Override public boolean validate(String id, String value, CustomerProfile profile) { /* logic */ }
    @Override public int getPriority() { return 80; }
    @Override public String normalizeTokenValue(String value) { return value.trim(); }
}
```

### Key Services
- **TokenValidationService**: Routes tokens to validators
- **AuthenticationContextService**: Manages session state  
- **TokenProcessingService**: Processes customer tokens
- **AuthenticationResponseService**: Builds responses

### Customer Data
```java
CustomerProfile profile = CustomerProfile.builder()
    .ssn("123456789")
    .hashedPin("$2a$10$...")
    .dateOfBirth(LocalDate.of(1990, 5, 15))
    .build();
```

## 🔧 Common Tasks

### Add New Validator
1. Create class implementing `TokenValidator`
2. Add `@Component` annotation
3. Implement required methods
4. Spring auto-discovers it!

### Add New Brand
```java
@Component
public class MyBankConfig implements BrandAuthConfiguration {
    @Override public String getBrandCode() { return "MY_BANK"; }
    @Override public List<String> getRequiredTokens() { return List.of("SSN", "PIN"); }
    // ... other methods
}
```

### Test Validator
```java
@Test
void shouldValidateToken() {
    // Given
    CustomerProfile profile = createTestProfile();
    
    // When  
    boolean result = validator.validate("customer1", "testValue", profile);
    
    // Then
    assertThat(result).isTrue();
}
```

## 🐛 Quick Debug

### Enable Debug Logging
```yaml
logging:
  level:
    com.bank.ivr.auth: DEBUG
```

### Common Issues
- Missing `@Component` → Validator not found
- Wrong token name → No validator found  
- Missing mock setup → NullPointerException

## 📊 Data Flow
```
Request → Context → Processing → Validation → Response
```

## 🎮 Priority System
- SSN: 100 (highest)
- PIN: 90  
- DOB: 70
- Custom: 50 (default)

Higher number = higher priority when conflicts exist. 