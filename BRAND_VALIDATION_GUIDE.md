# Brand-Aware Token Validation Guide

## Overview

The IVR Authentication System now supports brand-aware token validation, enforcing the rule that there can only be one token validator per token per brand. This allows different banks/brands to have their own validation logic while maintaining system integrity.

## Key Changes

### Enhanced TokenValidator Interface

The TokenValidator interface now requires a getBrand() method:

```java
public interface TokenValidator {
    String getTokenName();    // Same as before
    String getBrand();        // NEW: Brand identification
    boolean validate(...);    // Same as before
}
```

### Composite Key System

The TokenValidationService now uses brand:token composite keys:
- DEFAULT:SSN - Default SSN validator
- COMMUNITY_BANK:SSN - Community Bank specific SSN validator
- PREMIUM_BANK:PIN - Premium Bank specific PIN validator

## Constraint Enforcement

### Allowed: Different Validators for Same Token Across Brands

You can have multiple validators for the same token as long as they serve different brands.

### Forbidden: Multiple Validators for Same Token and Brand

The system will throw an IllegalStateException at startup if multiple validators are found for the same brand+token combination.

## Migration Guide

All existing validators must implement the new getBrand() method. Use "DEFAULT" for existing validators that should work across all brands.

## Benefits

1. Brand Isolation: Each brand can have different validation rules
2. System Integrity: Prevents conflicts and ensures predictable behavior  
3. Flexibility: Easy to add brand-specific requirements
4. Backward Compatibility: Existing code continues to work
5. Clear Debugging: Logs show exactly which validator is used for each brand+token 