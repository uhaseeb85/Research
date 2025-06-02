# 🎯 Validator Selection Guide

## Overview

This guide explains exactly how the TokenValidationService selects validators using a **composite key system** with **smart fallback logic**. Understanding this system is crucial for implementing brand-specific validators and debugging authentication issues.

---

## 📋 Quick Reference

| **Question** | **Answer** |
|-------------|------------|
| **How are validators selected?** | 2-step lookup: 1) Brand-specific key (`"BRAND:TOKEN"`), 2) Fallback to DEFAULT (`"DEFAULT:TOKEN"`) |
| **How do we determine if brand-specific validator exists?** | Check if `validatorMap.containsKey("BRAND:TOKEN")` returns true |
| **When do we select non-brand (DEFAULT) validator?** | When brand-specific doesn't exist OR when brand is explicitly "DEFAULT" |

---

## 🔍 1. Validator Registration Process

### Startup Registration
During application startup, all `@Component` classes implementing `TokenValidator` are automatically registered:

```java
@Autowired
public TokenValidationService(List<TokenValidator> validators, ...) {
    this.validatorMap = new HashMap<>();
    
    // Loop through all validators and build the map
    for (TokenValidator validator : validators) {
        String tokenName = validator.getTokenName();    // e.g., "SSN"
        String brand = validator.getBrand();            // e.g., "PREMIUM_BANK" or "DEFAULT"
        String compositeKey = brand + ":" + tokenName;  // e.g., "PREMIUM_BANK:SSN"
        
        validatorMap.put(compositeKey, validator);
        logger.debug("Registered validator for brand '{}', token '{}' with priority {}", 
                    brand, tokenName, validator.getPriority());
    }
}
```

### Resulting Validator Map
```
"DEFAULT:SSN"                    → SsnValidator
"DEFAULT:DEBIT_CARD_PIN"        → DebitCardPinValidator  
"DEFAULT:DATE_OF_BIRTH"         → DateOfBirthValidator
"DEFAULT:MOTHER_MAIDEN_NAME"    → MotherMaidenNameValidator
"DIGITAL_BANK:FACE_ID"          → DigitalBankFaceIdValidator
"PREMIUM_BANK:SSN"              → PremiumBankSsnValidator (overrides DEFAULT for PREMIUM_BANK)
"COMMUNITY_BANK:DEBIT_CARD_PIN" → CommunityBankPinValidator (overrides DEFAULT for COMMUNITY_BANK)
```

---

## 🎯 2. Validator Selection Logic

### Core Selection Method
The heart of validator selection is the `getValidatorForBrandAndToken()` method:

```java
private TokenValidator getValidatorForBrandAndToken(String brand, String tokenName) {
    // STEP 1: First try brand-specific validator
    TokenValidator validator = validatorMap.get(createCompositeKey(brand, tokenName));
    
    // STEP 2: If not found and brand is not DEFAULT, try DEFAULT brand as fallback
    if (validator == null && !"DEFAULT".equals(brand)) {
        validator = validatorMap.get(createCompositeKey("DEFAULT", tokenName));
        if (validator != null) {
            logger.debug("Using DEFAULT validator for brand '{}' and token '{}'", brand, tokenName);
        }
    }
    
    return validator;
}

private String createCompositeKey(String brand, String tokenName) {
    return brand + ":" + tokenName;
}
```

### Decision Tree
```
INPUT: validateToken(tokenName, brand, ...)
│
├─ STEP 1: validatorMap.get(brand + ":" + tokenName)
│  ├─ Found? → ✅ USE IT!
│  └─ Not Found? → Continue to STEP 2
│
├─ STEP 2: Is brand == "DEFAULT"?
│  ├─ YES → ❌ Return null (no fallback for DEFAULT)
│  └─ NO → validatorMap.get("DEFAULT:" + tokenName)
│     ├─ Found? → ✅ USE IT! (log fallback)
│     └─ Not Found? → ❌ Return null
│
└─ RESULT:
   ├─ Validator found → Perform validation
   └─ No validator → Return false
```

---

## 🔄 3. Real Examples of Validator Selection

### Scenario A: Brand-Specific Validator Exists
```java
// Request: validateToken("SSN", "PREMIUM_BANK", ...)
// STEP 1: Look for "PREMIUM_BANK:SSN" → ✅ Found PremiumBankSsnValidator
// STEP 2: Not needed
// RESULT: Uses PremiumBankSsnValidator (requires full SSN, premium customer check)
```

### Scenario B: Brand-Specific Validator Does NOT Exist - Fallback to DEFAULT
```java
// Request: validateToken("SSN", "ROYAL_BANK", ...)  
// STEP 1: Look for "ROYAL_BANK:SSN" → ❌ Not found
// STEP 2: Look for "DEFAULT:SSN" → ✅ Found SsnValidator
// RESULT: Uses SsnValidator (accepts full SSN or last 4 digits)
// LOG: "Using DEFAULT validator for brand 'ROYAL_BANK' and token 'SSN'"
```

### Scenario C: No Validator Exists at All
```java
// Request: validateToken("FINGERPRINT", "ANY_BANK", ...)
// STEP 1: Look for "ANY_BANK:FINGERPRINT" → ❌ Not found  
// STEP 2: Look for "DEFAULT:FINGERPRINT" → ❌ Not found
// RESULT: Returns null → Validation fails
```

### Scenario D: DEFAULT Brand Request (No Fallback Needed)
```java
// Request: validateToken("SSN", "DEFAULT", ...)
// STEP 1: Look for "DEFAULT:SSN" → ✅ Found SsnValidator
// STEP 2: Not needed (brand is already DEFAULT)
// RESULT: Uses SsnValidator
```

---

## 🔧 4. Determining If Brand-Specific Validator Exists

### Method 1: Direct Check (No Fallback)
```java
// Check exact existence
boolean hasSpecific = tokenValidationService.hasValidator("PREMIUM_BANK", "SSN");
// Returns: true (exact match exists)

boolean hasSpecific = tokenValidationService.hasValidator("ROYAL_BANK", "SSN");  
// Returns: false (no exact match)
```

### Method 2: Validator Lookup (No Fallback)
```java
// Get exact validator
TokenValidator specific = tokenValidationService.getValidator("PREMIUM_BANK", "SSN");
// Returns: PremiumBankSsnValidator (exact match)

TokenValidator specific = tokenValidationService.getValidator("ROYAL_BANK", "SSN");
// Returns: null (no exact match)
```

### Method 3: Validation with Fallback
```java
// Perform validation (includes fallback)
boolean result = tokenValidationService.validateToken("SSN", "ROYAL_BANK", ...);
// Internally: 1) Try ROYAL_BANK:SSN → null, 2) Try DEFAULT:SSN → SsnValidator
// Returns: true/false based on validation, NOT null
```

---

## 🚨 5. Key Behavior Differences

| **Method** | **Includes Fallback?** | **Returns When No Match** | **Use Case** |
|------------|------------------------|---------------------------|--------------|
| `validateToken()` | ✅ **YES** | `false` (validation failed) | **Actual authentication** |
| `getValidator()` | ❌ **NO** | `null` | **Check specific brand support** |
| `hasValidator()` | ❌ **NO** | `false` | **Configuration validation** |

### Code Examples
```java
// EXACT LOOKUP (no fallback) - for capability checking
TokenValidator royal = tokenValidationService.getValidator("ROYAL_BANK", "SSN");
// → null (ROYAL_BANK has no specific SSN validator)

// VALIDATION (with fallback) - for actual authentication  
boolean valid = tokenValidationService.validateToken("SSN", "ROYAL_BANK", "123456789", profile);
// → true/false (falls back to DEFAULT:SSN validator)
// LOG: "Using DEFAULT validator for brand 'ROYAL_BANK' and token 'SSN'"
```

---

## 🔄 6. When Non-Brand (DEFAULT) Validators Are Selected

DEFAULT validators are selected in these cases:

### Case 1: Explicit DEFAULT Brand Request
```java
validateToken("SSN", "DEFAULT", ...)  // Direct DEFAULT request
// STEP 1: Look for "DEFAULT:SSN" → ✅ Found SsnValidator
// STEP 2: Not needed
// RESULT: Uses SsnValidator
```

### Case 2: Brand-Specific Validator Doesn't Exist (Fallback)
```java
validateToken("SSN", "ROYAL_BANK", ...)  // Falls back to DEFAULT:SSN
// STEP 1: Look for "ROYAL_BANK:SSN" → ❌ Not found
// STEP 2: Look for "DEFAULT:SSN" → ✅ Found SsnValidator
// RESULT: Uses SsnValidator with fallback logging
```

### Case 3: Brand Exists But Doesn't Have That Specific Token
```java
validateToken("DATE_OF_BIRTH", "PREMIUM_BANK", ...)  // Falls back to DEFAULT:DATE_OF_BIRTH
// STEP 1: Look for "PREMIUM_BANK:DATE_OF_BIRTH" → ❌ Not found
// STEP 2: Look for "DEFAULT:DATE_OF_BIRTH" → ✅ Found DateOfBirthValidator
// RESULT: Uses DateOfBirthValidator
```

---

## 🎯 7. Brand-Specific Validator Examples

### DIGITAL_BANK Face ID Validator
```java
@Component
public class DigitalBankFaceIdValidator implements TokenValidator {
    
    @Override
    public String getTokenName() {
        return "FACE_ID";
    }
    
    @Override
    public String getBrand() {
        return "DIGITAL_BANK"; // Creates composite key: "DIGITAL_BANK:FACE_ID"
    }
    
    // Advanced biometric validation with higher threshold
    // Mobile app enrollment requirement
    // Age verification for legal compliance
}
```

### PREMIUM_BANK SSN Validator (Stricter)
```java
@Component
public class PremiumBankSsnValidator implements TokenValidator {
    
    @Override
    public String getTokenName() {
        return "SSN";
    }
    
    @Override
    public String getBrand() {
        return "PREMIUM_BANK"; // Creates composite key: "PREMIUM_BANK:SSN"
    }
    
    // Full 9-digit SSN required (no partial matching)
    // Premium customer eligibility check
    // Enhanced security audit logging
}
```

### COMMUNITY_BANK PIN Validator (Flexible)
```java
@Component
public class CommunityBankPinValidator implements TokenValidator {
    
    @Override
    public String getTokenName() {
        return "DEBIT_CARD_PIN";
    }
    
    @Override
    public String getBrand() {
        return "COMMUNITY_BANK"; // Creates composite key: "COMMUNITY_BANK:DEBIT_CARD_PIN"
    }
    
    // Flexible PIN formats (4-6 digits)
    // Customer service focused logging
    // Community-friendly validation approach
}
```

---

## 🎯 8. Smart Lookup with Fallback

### Composite Key Registration
When the application starts, validators are registered using composite keys:

```
Validator Map Keys:
DEFAULT:SSN                    → SsnValidator (fallback)
DEFAULT:DEBIT_CARD_PIN        → DebitCardPinValidator (fallback)  
DEFAULT:DATE_OF_BIRTH         → DateOfBirthValidator (fallback)
DEFAULT:MOTHER_MAIDEN_NAME    → MotherMaidenNameValidator (fallback)
DIGITAL_BANK:FACE_ID          → DigitalBankFaceIdValidator (brand-specific)
PREMIUM_BANK:SSN              → PremiumBankSsnValidator (brand-specific, overrides default)
COMMUNITY_BANK:DEBIT_CARD_PIN → CommunityBankPinValidator (brand-specific, overrides default)
```

### Lookup Logic Examples

#### Example 1: DIGITAL_BANK Customer Using Face ID
```
Request: validateToken("FACE_ID", "DIGITAL_BANK", ...)
Lookup: "DIGITAL_BANK:FACE_ID" → ✅ DigitalBankFaceIdValidator
Result: Advanced biometric validation with 95% threshold + mobile app requirement
```

#### Example 2: PREMIUM_BANK Customer Using SSN
```
Request: validateToken("SSN", "PREMIUM_BANK", ...)  
Lookup: "PREMIUM_BANK:SSN" → ✅ PremiumBankSsnValidator
Result: Full 9-digit SSN required + premium customer eligibility check + audit logging
```

#### Example 3: ROYAL_BANK Customer Using SSN (Fallback)
```
Request: validateToken("SSN", "ROYAL_BANK", ...)
Lookup: "ROYAL_BANK:SSN" → ❌ Not found
Fallback: "DEFAULT:SSN" → ✅ SsnValidator  
Result: Standard SSN validation (full or last-4 digits)
```

#### Example 4: COMMUNITY_BANK Customer Using PIN
```
Request: validateToken("DEBIT_CARD_PIN", "COMMUNITY_BANK", ...)
Lookup: "COMMUNITY_BANK:DEBIT_CARD_PIN" → ✅ CommunityBankPinValidator
Result: Flexible PIN validation (4-6 digits) + customer service focus
```

---

## ✅ 9. Benefits of This Approach

| **Feature** | **Benefit** |
|-------------|-------------|
| **Brand Differentiation** | Each bank can have unique validation requirements |
| **Security Levels** | Premium banks can enforce stricter validation |
| **Customer Experience** | Community banks can be more lenient and customer-friendly |
| **Backward Compatibility** | DEFAULT validators ensure no brand is left without validation |
| **Zero Infrastructure Changes** | Existing composite key system handles everything automatically |
| **Easy Deployment** | Just add `@Component` and implement `TokenValidator` |
| **Smart Fallback** | System gracefully handles missing brand-specific validators |

---

## 🛠️ 10. Implementation Checklist

### To Create a Brand-Specific Validator:

1. **Create the validator class:**
   ```java
   @Component
   public class YourBrandTokenValidator implements TokenValidator {
       
       @Override
       public String getTokenName() {
           return "YOUR_TOKEN";  // e.g., "SSN", "PIN", "BIOMETRIC"
       }
       
       @Override
       public String getBrand() {
           return "YOUR_BRAND";  // e.g., "PREMIUM_BANK", "DIGITAL_BANK"
       }
       
       @Override
       public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
           // Your brand-specific validation logic
       }
   }
   ```

2. **Automatic registration:** Spring will automatically detect and register your validator

3. **Composite key creation:** System automatically creates `"YOUR_BRAND:YOUR_TOKEN"` key

4. **Fallback behavior:** If validation fails, system gracefully falls back to `"DEFAULT:YOUR_TOKEN"`

### To Check Validator Selection:

1. **Check specific brand support:**
   ```java
   boolean hasSpecific = tokenValidationService.hasValidator("YOUR_BRAND", "YOUR_TOKEN");
   ```

2. **View all registered validators:**
   ```java
   Set<String> keys = tokenValidationService.getSupportedBrandTokenCombinations();
   ```

3. **Test validation with logging:**
   ```java
   boolean result = tokenValidationService.validateToken("YOUR_TOKEN", "YOUR_BRAND", ...);
   // Check logs for fallback messages
   ```

---

## 🔍 11. Debugging Tips

### Common Issues and Solutions:

1. **Validator not found:**
   - Check if `@Component` annotation is present
   - Verify `getTokenName()` and `getBrand()` return correct values
   - Check application logs for registration messages

2. **Unexpected fallback behavior:**
   - Use `getValidator()` to check exact matches vs fallback
   - Check logs for "Using DEFAULT validator" messages
   - Verify composite key format: `"BRAND:TOKEN"`

3. **Multiple validators for same brand+token:**
   - System will throw `IllegalStateException` during startup
   - Only one validator per brand+token combination is allowed
   - Remove duplicate validators or differentiate token names

### Useful Debug Commands:
```java
// Check what validators are registered
Set<String> allValidators = tokenValidationService.getSupportedBrandTokenCombinations();

// Check specific brand support  
boolean hasValidator = tokenValidationService.hasValidator("BRAND", "TOKEN");

// Get specific validator (no fallback)
TokenValidator validator = tokenValidationService.getValidator("BRAND", "TOKEN");

// Test validation (with fallback and logging)
boolean result = tokenValidationService.validateToken("TOKEN", "BRAND", customerValue, tokenValue, profile);
```

---

This validator selection system provides a robust, flexible foundation for implementing brand-specific authentication logic while maintaining backward compatibility and graceful fallback behavior. 🎯 