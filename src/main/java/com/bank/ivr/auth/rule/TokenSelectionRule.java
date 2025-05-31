package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;

/**
 * Interface for rules that determine which specific authentication token to ask for next.
 * 
 * These rules are evaluated during response building to make intelligent token selection
 * decisions based on complex business logic, customer profile attributes, trust levels,
 * and brand-specific requirements.
 * 
 * EXECUTION PATTERN:
 * - Rules are evaluated in priority order (highest priority first)
 * - First rule that returns a non-null token wins ("first match wins")
 * - Remaining rules are skipped for performance
 * - If no rules apply, system falls back to priority-based token selection
 * 
 * IMPLEMENTATION GUIDELINES:
 * - Return null if this rule doesn't apply to the current scenario
 * - Return specific token name if this rule should override default selection
 * - Consider brand filtering using getBrand() method
 * - Ensure isApplicable() correctly identifies when rule should run
 * - Handle exceptions gracefully (system will catch and continue)
 * 
 * COMMON USE CASES:
 * - Trust level based token selection (RED trust = full SSN, GREEN = last 4)
 * - Brand-specific token preferences (mobile banks prefer app PIN)
 * - Customer risk profile based selection (high-value customers need stronger auth)
 * - Regional or demographic based token selection
 * - Compliance or regulatory requirement based selection
 */
public interface TokenSelectionRule {
    
    /**
     * Determines the next authentication token to request based on business logic.
     * 
     * This is the core method where business rules are implemented to intelligently
     * select which token to ask for based on the current authentication context,
     * customer profile, trust levels, and other factors.
     * 
     * IMPLEMENTATION NOTES:
     * - Return null if this rule doesn't apply to current scenario
     * - Return specific token name (e.g., "SSN", "DEBIT_CARD_PIN") if rule applies
     * - System will validate token availability (eligibility, attempts, etc.)
     * - Consider trust level info: context.getTrustLevelInfo()
     * - Consider customer attributes: customerProfile.getAccountStatus(), etc.
     * - Consider authentication progress: context.getAuthenticatedTokens()
     * 
     * EXAMPLES:
     * - if (trustInfo.isLowTrust()) return "SSN_FULL";
     * - if (customerProfile.isHighValue()) return "BIOMETRIC";
     * - if (context.getAuthenticatedTokens().isEmpty()) return "PRIMARY_TOKEN";
     * 
     * @param context the current authentication context containing session info,
     *                trust levels, authenticated tokens, failed tokens, etc.
     * @param customerProfile the customer's profile data including account status,
     *                       demographics, risk indicators, etc.
     * @return the token name to ask next (e.g., "SSN", "DEBIT_CARD_PIN"), 
     *         or null if this rule doesn't apply to the current scenario
     */
    String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile);
    
    /**
     * Determines if this rule should be evaluated for the current authentication scenario.
     * 
     * This method acts as a pre-filter to improve performance by avoiding expensive
     * rule evaluation when the rule clearly doesn't apply to the current context.
     * 
     * PERFORMANCE OPTIMIZATION:
     * - Called before determineNextToken() to avoid unnecessary computation
     * - Should be fast and lightweight (avoid complex calculations)
     * - Return false early if rule clearly doesn't apply
     * 
     * COMMON FILTERS:
     * - Brand compatibility: if (!BRAND_CODE.equals(context.getBrand())) return false;
     * - Customer status: if (!"ACTIVE".equals(customerProfile.getAccountStatus())) return false;
     * - Trust level requirement: if (context.getTrustLevelInfo() == null) return false;
     * - Authentication state: if (context.getAuthenticatedTokens().size() >= 2) return false;
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @return true if this rule should be evaluated (determineNextToken will be called),
     *         false if rule doesn't apply (skip this rule for performance)
     */
    boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile);
    
    /**
     * Handles token validation failure scenarios and determines escalation strategy.
     * 
     * This method is called when a token that was previously selected by this rule
     * (or the system) fails validation. It allows the rule to implement intelligent
     * escalation or fallback strategies.
     * 
     * ESCALATION STRATEGIES:
     * - Escalate to stronger authentication: PIN failure → full SSN
     * - Switch to alternative method: SSN failure → Biometric
     * - Require additional verification: any failure → multi-factor
     * - Return null to let system handle with default logic
     * 
     * SECURITY CONSIDERATIONS:
     * - Don't escalate infinitely (check attempt counts)
     * - Consider trust level when escalating
     * - Respect brand security policies
     * - Log escalation decisions for audit purposes
     * 
     * DEFAULT BEHAVIOR:
     * - Base implementation returns null (no special handling)
     * - System will use default failure handling logic
     * 
     * @param context the current authentication context
     * @param customerProfile the customer's profile data
     * @param failedToken the name of the token that failed validation
     * @return the escalation token name to try next, or null to use default handling
     */
    default String handleTokenFailure(AuthenticationContext context, CustomerProfile customerProfile, String failedToken) {
        return null; // Default behavior: no escalation, use system default logic
    }
    
    /**
     * Returns the brand code this rule applies to for brand-specific filtering.
     * 
     * BRAND FILTERING:
     * - Return specific brand code (e.g., "PREMIUM_BANK") for brand-specific rules
     * - Return "DEFAULT" for rules that apply to all brands
     * - System automatically filters rules based on current authentication brand
     * 
     * PERFORMANCE IMPACT:
     * - Rules are filtered by brand before evaluation for efficiency
     * - Only applicable brand rules are considered during token selection
     * 
     * @return the brand code (e.g., "PREMIUM_BANK", "COMMUNITY_BANK") or "DEFAULT" for all brands
     */
    default String getBrand() {
        return "DEFAULT";
    }
    
    /**
     * Returns a descriptive name for this rule used in logging, debugging, and monitoring.
     * 
     * NAMING CONVENTIONS:
     * - Use UPPER_CASE_WITH_UNDERSCORES format
     * - Include brand if brand-specific: "ROYAL_BANK_TRUST_BASED_SSN"
     * - Be descriptive about rule purpose: "HIGH_VALUE_CUSTOMER_BIOMETRIC"
     * - Keep reasonably short for log readability
     * 
     * USAGE:
     * - Appears in debug logs for troubleshooting
     * - Used in monitoring and analytics
     * - Helps identify which rule selected specific tokens
     * 
     * @return descriptive rule name for logging and debugging (e.g., "TRUST_BASED_TOKEN_SELECTION")
     */
    String getRuleName();
    
    /**
     * Returns the priority of this rule for execution ordering.
     * 
     * PRIORITY SYSTEM:
     * - Higher numbers = higher priority (evaluated first)
     * - Rules evaluated in descending priority order
     * - First rule returning non-null token wins
     * - Typical ranges: 0-50 (low), 51-100 (medium), 101+ (high)
     * 
     * PRIORITY GUIDELINES:
     * - Brand-specific rules: 150-200 (very high priority)
     * - Security/compliance rules: 100-149 (high priority)
     * - Business logic rules: 50-99 (medium priority)
     * - Default/fallback rules: 0-49 (low priority)
     * 
     * EXAMPLES:
     * - 200: Critical brand-specific security rules
     * - 150: Trust-based authentication rules
     * - 100: High-value customer rules
     * - 50: General business preference rules
     * - 10: Default fallback rules
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
     * - Used in system documentation and API docs
     * - Assists in troubleshooting and debugging
     * - Provides context for rule behavior in logs
     * 
     * DESCRIPTION GUIDELINES:
     * - Explain the business logic or conditions
     * - Mention key decision factors (trust level, customer type, etc.)
     * - Keep concise but informative
     * - Use business terms, not technical jargon
     * 
     * @return human-readable description of rule conditions and behavior
     */
    default String getConditionDescription() {
        return "No description provided";
    }
} 