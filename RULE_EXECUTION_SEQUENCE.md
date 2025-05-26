# Rule Execution Sequence & Control

## 🎯 Two Distinct Rule Types

You're absolutely right that there are **two distinct types of rules** that need to be separated:

### 1. **Eligibility Rules** - "Can this customer use this token?"
- **Purpose**: Determine which authentication methods are available to a customer
- **When**: Called during initial context creation (once per session)
- **Interface**: `EligibilityRule`
- **Examples**: `SsnEligibilityRule`, `DebitCardPinEligibilityRule`

### 2. **Token Selection Rules** - "Which token should we ask for next?"
- **Purpose**: Decide the specific token to request based on complex business logic
- **When**: Called during response building (every time we need to ask for a token)
- **Interface**: `TokenSelectionRule`
- **Examples**: `RoyalBankTrustBasedSsnRule`, `FullAuthenticationCompletionRule`

---

## 🔄 Complete Rule Execution Sequence

### **Phase 1: Session Initialization**
```
1. Customer calls in
2. EligibilityService.determineEligibleTokens() called
3. Eligibility rules evaluated (no specific order needed)
4. Available tokens stored in AuthenticationContext.eligibleTokens
```

### **Phase 2: Token Selection (Every Request)**
```
1. AuthenticationResponseService.buildResponse() called
2. TokenSelectionService.determineNextToken() called
3. Token selection rules evaluated BY PRIORITY (highest first)
4. First applicable rule wins
5. Fallback to priority-based selection if no rules apply
```

### **Phase 3: Failure Handling**
```
1. Token validation fails
2. TokenSelectionService.handleTokenFailure() called
3. Token selection rules evaluated BY PRIORITY for escalation
4. First rule providing escalation wins
5. Fallback to BrandFailurePolicyService if no rules handle it
```

---

## 🎛️ Controlling Rule Execution Sequence

### **Priority-Based Ordering**

Rules are executed in **priority order** (highest number first):

```java
@Component
public class RoyalBankTrustBasedSsnRule implements TokenSelectionRule {
    
    @Override
    public int getPriority() {
        return 200; // Higher priority = evaluated first
    }
}

@Component
public class DefaultSsnRule implements TokenSelectionRule {
    
    @Override
    public int getPriority() {
        return 100; // Lower priority = evaluated later
    }
}
```

**Execution Order:**
1. `RoyalBankTrustBasedSsnRule` (priority: 200) ✅ **Evaluated first**
2. `DefaultSsnRule` (priority: 100) - Only if first rule doesn't apply

### **Brand Filtering**

Rules can be brand-specific or apply to all brands:

```java
@Override
public String getBrand() {
    return "ROYAL_BANK"; // Only applies to Royal Bank
}

@Override
public String getBrand() {
    return "DEFAULT"; // Applies to all brands
}
```

### **Applicability Checks**

Each rule determines if it should be evaluated:

```java
@Override
public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
    // Only apply to Royal Bank
    if (!BRAND_CODE.equals(context.getBrand())) {
        return false;
    }
    
    // Must have trust level info
    if (context.getTrustLevelInfo() == null) {
        return false;
    }
    
    // Customer must have SSN on record
    return customerProfile.getSsn() != null 
           && !customerProfile.getSsn().trim().isEmpty()
           && "ACTIVE".equals(customerProfile.getAccountStatus());
}
```

---

## 📋 Current Rule Inventory

### **Eligibility Rules** (Phase 1)
| Rule Name | Token | Priority | Purpose |
|-----------|-------|----------|---------|
| `SsnEligibilityRule` | SSN | 100 | Check if customer has SSN on record |
| `DebitCardPinEligibilityRule` | DEBIT_CARD_PIN | 90 | Check if customer has PIN on record |

### **Token Selection Rules** (Phase 2 & 3)
| Rule Name | Brand | Priority | Purpose |
|-----------|-------|----------|---------|
| `FullAuthenticationCompletionRule` | ALL | 1000 | Determine if auth is complete |
| `RoyalBankTrustBasedSsnRule` | ROYAL_BANK | 200 | Trust-based SSN selection |

---

## 🚀 How to Add New Rules

### **Adding an Eligibility Rule**

```java
@Component
public class BiometricEligibilityRule implements EligibilityRule {
    
    @Override
    public boolean isEligible(CustomerProfile customerProfile, String brand) {
        return customerProfile.getBiometricHash() != null 
               && !customerProfile.getBiometricHash().trim().isEmpty()
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getTokenName() {
        return "BIOMETRIC";
    }
    
    @Override
    public String getRuleName() {
        return "BIOMETRIC_ELIGIBILITY";
    }
    
    @Override
    public int getPriority() {
        return 150; // Higher than SSN
    }
}
```

### **Adding a Token Selection Rule**

```java
@Component
public class TechBankMobileFirstRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        if (!isApplicable(context, customerProfile)) {
            return null;
        }
        
        // Tech Bank prefers mobile authentication
        if (context.getEligibleTokens().contains("MOBILE_PIN")) {
            return "MOBILE_PIN";
        }
        
        return "SSN"; // Fallback
    }
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
        return "TECH_BANK".equals(context.getBrand()) 
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getBrand() {
        return "TECH_BANK";
    }
    
    @Override
    public String getRuleName() {
        return "TECH_BANK_MOBILE_FIRST";
    }
    
    @Override
    public int getPriority() {
        return 180; // High priority for brand-specific logic
    }
}
```

---

## 🔧 Integration Points

### **Where Eligibility Rules Are Called**

```java
// In EligibilityService.determineEligibleTokens()
for (EligibilityRule rule : eligibilityRules) {
    if (rule.isEligible(customerProfile, brand)) {
        eligibleTokens.add(rule.getTokenName());
    }
}
```

### **Where Token Selection Rules Are Called**

```java
// In TokenSelectionService.determineNextToken()
List<TokenSelectionRule> applicableRules = getApplicableRules(context, customerProfile, brand);

for (TokenSelectionRule rule : applicableRules) { // Already sorted by priority
    String selectedToken = rule.determineNextToken(context, customerProfile);
    if (selectedToken != null) {
        return findTokenDefinition(selectedToken, brandTokenDefinitions);
    }
}
```

### **Where Failure Handling Rules Are Called**

```java
// In TokenSelectionService.handleTokenFailure()
for (TokenSelectionRule rule : applicableRules) { // Already sorted by priority
    String escalationToken = rule.handleTokenFailure(context, customerProfile, failedToken);
    if (escalationToken != null) {
        return findTokenDefinition(escalationToken, brandTokenDefinitions);
    }
}
```

---

## 🎯 Benefits of This Separation

### **Clear Responsibilities**
- **Eligibility Rules**: Simple yes/no decisions
- **Token Selection Rules**: Complex business logic

### **Proper Execution Order**
- **Eligibility**: No ordering needed (just checking availability)
- **Token Selection**: Priority-based ordering ensures correct rule precedence

### **Brand Awareness**
- Rules can be brand-specific or universal
- Automatic filtering by brand during evaluation

### **Scalability**
- Easy to add new rules without modifying existing code
- Clear interfaces make testing straightforward

### **Maintainability**
- Each rule is self-contained
- Easy to understand what each rule does
- Clear separation of concerns

---

## 🔍 Example: Royal Bank Trust-Based Flow

### **Step 1: Eligibility (Session Start)**
```
1. SsnEligibilityRule.isEligible() → true (customer has SSN)
2. DebitCardPinEligibilityRule.isEligible() → true (customer has PIN)
3. Context.eligibleTokens = ["SSN", "DEBIT_CARD_PIN"]
```

### **Step 2: Token Selection (First Request)**
```
1. RoyalBankTrustBasedSsnRule.isApplicable() → true (Royal Bank + trust info)
2. RoyalBankTrustBasedSsnRule.determineNextToken() → "SSN_LAST_4" (green trust, no phone match)
3. Ask customer for last 4 digits of SSN
```

### **Step 3: Failure Handling (If SSN_LAST_4 fails)**
```
1. RoyalBankTrustBasedSsnRule.handleTokenFailure("SSN_LAST_4") → "SSN_FULL"
2. Ask customer for full SSN
```

This gives you **complete control** over the rule execution sequence while maintaining clean separation of concerns! 