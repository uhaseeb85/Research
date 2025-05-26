# Royal Bank Trust-Based Authentication System

## Overview

This document explains how the enhanced IVR Authentication system can handle Royal Bank's complex trust-level and phone matching requirements. The system has been extended to support hundreds of conditional authentication scenarios based on trust levels (RED/GREEN) and phone number matching status.

## System Enhancements

### 1. Trust Level Information Model

The system now captures trust level and phone matching data through the `TrustLevelInfo` class:

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
    private final Integer matchedSsnCount;
}
```

### 2. Enhanced Request Model

The `AuthenticationRequest` now includes trust level information:

```java
public class AuthenticationRequest {
    // ... existing fields
    private final TrustLevelInfo trustLevelInfo;
}
```

### 3. Conditional Authentication Rules

The new `ConditionalAuthenticationRule` interface enables complex business logic:

```java
public interface ConditionalAuthenticationRule extends AuthenticationRule {
    String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile);
    boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile);
    String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken);
    String getConditionDescription();
}
```

## Royal Bank Implementation

### Token Definitions

Royal Bank supports the following authentication tokens:

1. **SSN_LAST_4** - Last 4 digits of SSN (for high trust scenarios)
2. **SSN_FULL** - Complete SSN (for low trust or high risk scenarios)
3. **DEBIT_CARD_PIN** - Alternative authentication method
4. **DATE_OF_BIRTH** - Fallback option

### Trust-Based Authentication Rules

The `RoyalBankTrustBasedSsnRule` implements the complex conditional logic:

#### Scenario Matrix

| Trust Level | Phone Match Status | Initial Token | Failure Escalation |
|-------------|-------------------|---------------|-------------------|
| GREEN | NOT_MATCHED | SSN_LAST_4 | → SSN_FULL |
| GREEN | SINGLE_MATCH | SSN_LAST_4 | → SSN_FULL |
| GREEN | MULTIPLE_MATCHES | SSN_LAST_4 | → SSN_FULL |
| RED | NOT_MATCHED | SSN_FULL | → FAIL |
| RED | SINGLE_MATCH | SSN_FULL | → FAIL |
| RED | MULTIPLE_MATCHES | SSN_FULL | → FAIL |

#### Example Scenarios

**Scenario 1: Green Trust + Phone Not Matched**
```java
if (trustInfo.isHighTrust() && !trustInfo.hasPhoneMatch()) {
    return SSN_LAST_4; // Ask for last 4 digits first
}
```

**Scenario 2: Red Trust + Multiple Phone Matches**
```java
if (trustInfo.isLowTrust() && trustInfo.hasMultiplePhoneMatches()) {
    return SSN_FULL; // Require full SSN immediately
}
```

**Scenario 3: Green Trust + Last 4 SSN Failed**
```java
if (SSN_LAST_4.equals(failedToken) && trustInfo.isHighTrust() && !trustInfo.hasPhoneMatch()) {
    return SSN_FULL; // Escalate to full SSN
}
```

## Scalability for Hundreds of Scenarios

### 1. Rule-Based Architecture

The system can easily handle hundreds of scenarios by:

- **Creating multiple conditional rules** for different business cases
- **Chaining rules** with different priorities
- **Combining conditions** using boolean logic

### 2. Example: Extended Scenario Matrix

```java
@Component
public class RoyalBankAdvancedTrustRule implements ConditionalAuthenticationRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        
        // Scenario Group 1: High Trust Scenarios
        if (trustInfo.isHighTrust()) {
            if (!trustInfo.hasPhoneMatch()) {
                return handleHighTrustNoMatch(context, customerProfile);
            } else if (trustInfo.hasSinglePhoneMatch()) {
                return handleHighTrustSingleMatch(context, customerProfile);
            } else if (trustInfo.hasMultiplePhoneMatches()) {
                return handleHighTrustMultipleMatches(context, customerProfile);
            }
        }
        
        // Scenario Group 2: Low Trust Scenarios
        if (trustInfo.isLowTrust()) {
            if (!trustInfo.hasPhoneMatch()) {
                return handleLowTrustNoMatch(context, customerProfile);
            } else if (trustInfo.hasSinglePhoneMatch()) {
                return handleLowTrustSingleMatch(context, customerProfile);
            } else if (trustInfo.hasMultiplePhoneMatches()) {
                return handleLowTrustMultipleMatches(context, customerProfile);
            }
        }
        
        return getDefaultToken(context, customerProfile);
    }
    
    private String handleHighTrustNoMatch(AuthenticationContext context, CustomerProfile customerProfile) {
        // Sub-scenarios based on customer profile
        if (customerProfile.getAccountStatus().equals("PREMIUM")) {
            return "SSN_LAST_4"; // Premium customers get easier auth
        } else if (hasRecentFailedAttempts(context)) {
            return "SSN_FULL"; // Recent failures require full SSN
        } else if (isBusinessHours()) {
            return "SSN_LAST_4"; // Business hours are lower risk
        } else {
            return "SSN_FULL"; // After hours require full SSN
        }
    }
    
    // ... additional scenario handlers
}
```

### 3. Configuration-Driven Rules

For even more scalability, rules can be configuration-driven:

```yaml
royal_bank_rules:
  trust_scenarios:
    - condition: "trustLevel == GREEN && phoneMatch == NOT_MATCHED && accountType == PREMIUM"
      token: "SSN_LAST_4"
      failure_escalation: "SSN_FULL"
    - condition: "trustLevel == GREEN && phoneMatch == NOT_MATCHED && timeOfDay == AFTER_HOURS"
      token: "SSN_FULL"
      failure_escalation: null
    - condition: "trustLevel == RED && phoneMatch == MULTIPLE_MATCHES"
      token: "SSN_FULL"
      failure_escalation: "DEBIT_CARD_PIN"
    # ... hundreds more scenarios
```

### 4. Dynamic Rule Composition

Rules can be composed dynamically based on multiple factors:

```java
@Component
public class DynamicRoyalBankRule implements ConditionalAuthenticationRule {
    
    private final List<RuleCondition> ruleConditions;
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        for (RuleCondition condition : ruleConditions) {
            if (condition.matches(context, customerProfile)) {
                return condition.getRecommendedToken();
            }
        }
        return getDefaultToken();
    }
}

public class RuleCondition {
    private final Predicate<AuthenticationContext> contextPredicate;
    private final Predicate<CustomerProfile> profilePredicate;
    private final String recommendedToken;
    
    public boolean matches(AuthenticationContext context, CustomerProfile profile) {
        return contextPredicate.test(context) && profilePredicate.test(profile);
    }
}
```

## Integration Example

### Request Format

```json
{
  "sessionId": "session-123",
  "customerIdentifier": {
    "type": "PHONE_NUMBER",
    "value": "+1234567890"
  },
  "brand": "ROYAL_BANK",
  "trustLevelInfo": {
    "trustLevel": "GREEN",
    "phoneMatchStatus": "NOT_MATCHED",
    "matchedSsnCount": 0
  },
  "providedTokens": []
}
```

### Response Flow

1. **Initial Request**: System receives trust level GREEN, phone NOT_MATCHED
2. **Rule Evaluation**: `RoyalBankTrustBasedSsnRule` determines to ask for SSN_LAST_4
3. **User Provides**: Last 4 digits of SSN
4. **Validation Fails**: Digits don't match customer record
5. **Failure Handling**: Rule escalates to SSN_FULL
6. **User Provides**: Complete SSN
7. **Validation Success**: Authentication completes

## Benefits

### 1. Flexibility
- **Easy to add new scenarios** without changing core logic
- **Rule priority system** handles conflicts
- **Conditional logic** supports complex business requirements

### 2. Maintainability
- **Separation of concerns** between rules and core system
- **Clear rule descriptions** for debugging and auditing
- **Testable components** for each scenario

### 3. Scalability
- **Hundreds of rules** can coexist
- **Performance optimized** through rule applicability checks
- **Configuration-driven** rules for non-developers

### 4. Auditability
- **Complete rule tracing** for compliance
- **Decision logging** for each authentication step
- **Rule condition descriptions** for transparency

## Conclusion

The enhanced IVR Authentication system can absolutely handle Royal Bank's complex trust-level and phone matching requirements. The system supports:

✅ **Trust level-based authentication flows**
✅ **Phone matching status integration**
✅ **Complex conditional logic with hundreds of scenarios**
✅ **Failure escalation and fallback strategies**
✅ **Brand-specific configuration and rules**
✅ **Scalable rule architecture**
✅ **Maintainable and testable code structure**

The implementation provides a solid foundation that can grow to support hundreds of authentication scenarios while maintaining performance, security, and maintainability. 