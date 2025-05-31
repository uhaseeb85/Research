package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;

/**
 * Interface for rules that determine if additional authentication tokens should be requested
 * after successful validation of a token.
 * 
 * These rules are evaluated after token validation succeeds and can trigger additional 
 * authentication requirements based on:
 * - Trust levels (RED/GREEN)
 * - Phone number matching status
 * - Customer profile attributes
 * - Risk assessment factors
 * - Brand-specific security policies
 * 
 * EXECUTION PATTERN:
 * - Called after each successful token validation
 * - Evaluated in priority order (highest priority first)
 * - First rule requiring additional tokens wins
 * - Used to implement step-up authentication based on risk
 * 
 * IMPLEMENTATION GUIDELINES:
 * - Return TokenValidationResult.success() if no additional tokens needed
 * - Return TokenValidationResult.requiresAdditionalTokens() if more auth needed
 * - Consider current authentication state (what tokens already validated)
 * - Implement risk-based logic (trust levels, customer attributes)
 * - Be specific about why additional tokens are required
 * 
 * COMMON POST-VALIDATION SCENARIOS:
 * - Low trust level (RED) requires additional verification after any token
 * - Phone mismatch triggers extra security checks
 * - High-value customers need multi-factor authentication
 * - Suspicious activity patterns require step-up authentication
 * - Regulatory requirements mandate dual authentication
 * - First-time device or location requires additional verification
 */
public interface PostValidationRule {
    
    /**
     * Determines if additional tokens should be requested after successful validation.
     * 
     * This is the core business logic method that implements risk-based authentication
     * decisions based on the validated token, current context, and customer profile.
     * 
     * IMPLEMENTATION CONSIDERATIONS:
     * - Check current authentication state: context.getAuthenticatedTokens()
     * - Evaluate trust level: context.getTrustLevelInfo()
     * - Consider customer risk factors: customerProfile attributes
     * - Review phone matching status for additional risk indicators
     * - Apply brand-specific security policies
     * - Consider time-based or location-based risk factors
     * 
     * RISK ASSESSMENT FACTORS:
     * - Trust level (RED = high risk, GREEN = low risk)
     * - Phone matching (NOT_MATCHED = higher risk)
     * - Customer value tier (high-value = more security)
     * - Account history (new accounts = more verification)
     * - Transaction patterns (unusual activity = step-up)
     * - Geographic factors (foreign access = additional checks)
     * 
     * RETURN VALUE GUIDELINES:
     * - TokenValidationResult.success(): Authentication complete, no additional tokens
     * - TokenValidationResult.requiresAdditionalTokens(reason): More authentication needed
     * - Include specific reason for audit trail and user communication
     * 
     * EXAMPLES:
     * - if (trustInfo.isLowTrust()) return TokenValidationResult.requiresAdditionalTokens("Low trust level requires additional verification");
     * - if (customerProfile.isHighValue() && context.getAuthenticatedTokens().size() < 2) return requiresAdditional("High-value account requires multi-factor authentication");
     * - if (!trustInfo.hasPhoneMatch()) return requiresAdditional("Phone verification required for additional security");
     * 
     * @param validatedToken the name of the token that was successfully validated
     * @param context the current authentication context including trust info, session state,
     *                and authentication progress
     * @param customerProfile the customer's profile data including value tier, account history,
     *                       and risk indicators
     * @return TokenValidationResult indicating if additional authentication is required
     */
    TokenValidationResult evaluatePostValidation(String validatedToken, 
                                                AuthenticationContext context, 
                                                CustomerProfile customerProfile);
    
    /**
     * Determines if this rule should be evaluated for the current authentication scenario.
     * 
     * This method acts as a performance optimization filter to avoid expensive rule
     * evaluation when the rule clearly doesn't apply to the current context.
     * 
     * PERFORMANCE OPTIMIZATION:
     * - Called before evaluatePostValidation() to avoid unnecessary computation
     * - Should be fast and lightweight (avoid complex calculations)
     * - Return false early if rule clearly doesn't apply
     * 
     * COMMON APPLICABILITY FILTERS:
     * - Token-specific rules: if (!TARGET_TOKEN.equals(validatedToken)) return false;
     * - Brand compatibility: if (!BRAND_CODE.equals(context.getBrand())) return false;
     * - Trust level dependency: if (context.getTrustLevelInfo() == null) return false;
     * - Customer type filtering: if (!customerProfile.isHighValue()) return false;
     * - Authentication state: if (context.getAuthenticatedTokens().size() > 1) return false;
     * 
     * APPLICABILITY SCENARIOS:
     * - Rule only applies to specific tokens (e.g., SSN validation triggers biometric)
     * - Rule only applies to certain customer types (high-value, new accounts)
     * - Rule only applies when trust information is available
     * - Rule only applies for specific brands with enhanced security
     * 
     * @param validatedToken the name of the token that was successfully validated
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return true if this rule should be evaluated (evaluatePostValidation will be called),
     *         false if rule doesn't apply (skip for performance)
     */
    boolean isApplicable(String validatedToken, 
                        AuthenticationContext context, 
                        CustomerProfile customerProfile);
    
    /**
     * Returns the brand code this rule applies to for brand-specific filtering.
     * 
     * BRAND FILTERING:
     * - Return specific brand code for brand-specific post-validation rules
     * - Return "DEFAULT" for rules that apply to all brands
     * - System automatically filters rules based on authentication brand
     * 
     * BRAND-SPECIFIC POST-VALIDATION EXAMPLES:
     * - Premium banks might require biometric after any token validation
     * - Community banks might accept single-factor for low amounts
     * - Regional banks might have different risk thresholds
     * 
     * @return the brand code this rule applies to, or "DEFAULT" for all brands
     */
    default String getBrand() {
        return "DEFAULT";
    }
    
    /**
     * Returns a descriptive name for this rule used in logging, debugging, and monitoring.
     * 
     * NAMING CONVENTIONS:
     * - Use descriptive format: "TRUST_BASED_ADDITIONAL_AUTH"
     * - Include brand if brand-specific: "PREMIUM_BANK_HIGH_VALUE_MULTI_FACTOR"
     * - Be specific about trigger conditions: "PHONE_MISMATCH_STEP_UP_AUTH"
     * - Use UPPER_CASE_WITH_UNDERSCORES format
     * 
     * USAGE:
     * - Appears in debug logs during post-validation evaluation
     * - Used in security audit trails
     * - Helps identify which rules triggered additional authentication
     * - Critical for compliance and security monitoring
     * 
     * @return descriptive rule name for logging and security auditing
     */
    String getRuleName();
    
    /**
     * Returns the priority of this rule for execution ordering.
     * 
     * PRIORITY SYSTEM:
     * - Higher numbers = higher priority (evaluated first)
     * - First rule requiring additional tokens wins
     * - Used to implement layered security policies
     * - Critical rules should have highest priority
     * 
     * PRIORITY GUIDELINES:
     * - Critical security rules: 200-250 (evaluate first)
     * - Regulatory compliance rules: 150-199 (high priority)
     * - Brand-specific rules: 100-149 (medium-high priority)
     * - General business rules: 50-99 (medium priority)
     * - Optional enhancement rules: 0-49 (low priority)
     * 
     * EXAMPLES:
     * - 250: Fraud detection rules (immediate step-up)
     * - 200: High-value account protection rules
     * - 150: Regulatory compliance requirements
     * - 100: Trust level based rules
     * - 50: Customer preference rules
     * 
     * @return rule priority (higher numbers = higher priority, evaluated first)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Returns a human-readable description of the conditions this rule handles.
     * 
     * DOCUMENTATION PURPOSE:
     * - Helps developers understand rule logic without reading implementation
     * - Used in system documentation and compliance reports
     * - Assists in troubleshooting and security auditing
     * - Provides context for rule behavior in security logs
     * 
     * DESCRIPTION GUIDELINES:
     * - Explain the business logic and security rationale
     * - Mention key decision factors (trust level, customer type, etc.)
     * - Include conditions that trigger additional authentication
     * - Use business and security terms, not technical jargon
     * 
     * EXAMPLES:
     * - "Requires additional authentication when trust level is RED (high risk)"
     * - "Triggers step-up authentication for high-value customers after any token validation"
     * - "Requires phone verification when customer phone number doesn't match records"
     * 
     * @return human-readable description of rule conditions and security purpose
     */
    default String getConditionDescription() {
        return "No description provided";
    }
    
    /**
     * Returns the tokens this rule can trigger additional authentication for.
     * 
     * TOKEN FILTERING:
     * - Return empty list if rule applies to ALL tokens
     * - Return specific token list if rule only applies to certain tokens
     * - Used for performance optimization during rule filtering
     * 
     * TOKEN-SPECIFIC SCENARIOS:
     * - Rule only applies after SSN validation (return ["SSN"])
     * - Rule only applies to PIN-based tokens (return ["DEBIT_CARD_PIN", "MOBILE_PIN"])
     * - Rule applies to all tokens (return empty list)
     * 
     * PERFORMANCE BENEFIT:
     * - System can filter rules by token before evaluation
     * - Avoids calling isApplicable() for irrelevant tokens
     * - Improves post-validation performance
     * 
     * @return list of token names this rule applies to, or empty list for all tokens
     */
    default java.util.List<String> getApplicableTokens() {
        return java.util.Collections.emptyList(); // Empty means applies to all tokens
    }
} 