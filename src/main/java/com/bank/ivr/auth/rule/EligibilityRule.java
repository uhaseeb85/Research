package com.bank.ivr.auth.rule;

import com.bank.ivr.auth.model.domain.CustomerProfile;

/**
 * Interface for rules that determine customer eligibility for specific authentication tokens.
 * 
 * These rules are evaluated during initial context creation to determine which authentication
 * methods are available to a customer based on their profile, account status, brand policies,
 * and data availability.
 * 
 * EXECUTION PATTERN:
 * - Called once per authentication session during initial setup
 * - All rules are evaluated (not short-circuited like token selection)
 * - Results determine the eligible tokens list for the entire session
 * - Failed eligibility means token won't be available for authentication
 * 
 * IMPLEMENTATION GUIDELINES:
 * - Return true only if customer definitely has the required data
 * - Return false if data is missing, invalid, or customer ineligible
 * - Consider brand-specific requirements using brand parameter
 * - Be conservative - false negatives are better than false positives
 * - Handle null/missing data gracefully
 * 
 * COMMON ELIGIBILITY CRITERIA:
 * - Data availability (customer has SSN, PIN, biometric data on record)
 * - Account status (active, not suspended, not fraud-flagged)
 * - Brand policies (some tokens only available for premium customers)
 * - Regulatory compliance (certain regions require specific auth methods)
 * - Security level requirements (high-value accounts need stronger auth)
 */
public interface EligibilityRule {
    
    /**
     * Determines if a customer is eligible for a specific authentication token type.
     * 
     * This method evaluates whether the customer has the necessary data and meets
     * the requirements to use a particular authentication method.
     * 
     * ELIGIBILITY CRITERIA TO CHECK:
     * - Data availability: Does customer have required data on record?
     * - Data quality: Is the data valid and not corrupted?
     * - Account status: Is account active and not suspended/frozen?
     * - Brand policies: Does brand allow this token for this customer type?
     * - Security requirements: Does customer meet security level requirements?
     * - Regulatory compliance: Is token allowed in customer's jurisdiction?
     * 
     * IMPLEMENTATION EXAMPLES:
     * - SSN: return customerProfile.getSsn() != null && !customerProfile.getSsn().isEmpty();
     * - PIN: return customerProfile.getHashedPin() != null && "ACTIVE".equals(customerProfile.getAccountStatus());
     * - Biometric: return customerProfile.getBiometricHash() != null && customerProfile.isBiometricsEnabled();
     * 
     * BRAND CONSIDERATIONS:
     * - Some brands may restrict certain tokens to premium customers
     * - Regional brands may have different data requirements
     * - Compliance requirements may vary by brand
     * 
     * ERROR HANDLING:
     * - Return false for any uncertainty or missing data
     * - Don't throw exceptions - handle null values gracefully
     * - Log warnings for suspicious data conditions
     * 
     * @param customerProfile the customer's complete profile data including personal info,
     *                       account status, preferences, and authentication data
     * @param brand the brand code for brand-specific eligibility policies
     * @return true if customer is eligible for this token type, false otherwise
     */
    boolean isEligible(CustomerProfile customerProfile, String brand);
    
    /**
     * Returns the authentication token name this rule determines eligibility for.
     * 
     * TOKEN NAMING CONVENTIONS:
     * - Use consistent naming with token definitions (e.g., "SSN", "DEBIT_CARD_PIN")
     * - Match exactly with AuthTokenDefinition names
     * - Use UPPER_CASE_WITH_UNDERSCORES format
     * - Be specific for variants (e.g., "SSN_FULL" vs "SSN_LAST_4")
     * 
     * SYSTEM INTEGRATION:
     * - Must match token names in brand configurations
     * - Used to link eligibility rules with token definitions
     * - Appears in authentication context eligible tokens list
     * 
     * @return the exact token name this rule evaluates eligibility for (e.g., "SSN", "DEBIT_CARD_PIN", "BIOMETRIC")
     */
    String getTokenName();
    
    /**
     * Returns the brand code this rule applies to for brand-specific filtering.
     * 
     * BRAND FILTERING:
     * - Return specific brand code for brand-specific eligibility rules
     * - Return "DEFAULT" for rules that apply to all brands
     * - System automatically filters rules based on customer's brand
     * 
     * BRAND-SPECIFIC ELIGIBILITY EXAMPLES:
     * - Premium banks might have different biometric requirements
     * - Community banks might allow lower security tokens
     * - Regional banks might have different data requirements
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
     * - Use format: TOKEN_NAME + "_ELIGIBILITY" (e.g., "SSN_ELIGIBILITY")
     * - Include brand if brand-specific: "PREMIUM_BANK_BIOMETRIC_ELIGIBILITY"
     * - Be descriptive about special conditions: "HIGH_VALUE_CUSTOMER_PIN_ELIGIBILITY"
     * 
     * USAGE:
     * - Appears in debug logs during eligibility evaluation
     * - Used in monitoring dashboards and analytics
     * - Helps identify which rules affect token availability
     * - Useful for troubleshooting authentication setup issues
     * 
     * @return descriptive rule name for logging and debugging (e.g., "SSN_ELIGIBILITY")
     */
    String getRuleName();
    
    /**
     * Returns the priority of this rule when multiple eligibility rules exist for the same token.
     * 
     * PRIORITY USAGE:
     * - Higher numbers = higher priority (evaluated first)
     * - Used when multiple rules evaluate eligibility for same token
     * - First rule returning false can short-circuit evaluation
     * - Helpful for performance optimization
     * 
     * PRIORITY GUIDELINES:
     * - Security/fraud checks: 150-200 (very high - evaluate first)
     * - Data availability checks: 100-149 (high priority)
     * - Brand policy checks: 50-99 (medium priority)
     * - Optional enhancements: 0-49 (low priority)
     * 
     * EXAMPLES:
     * - 200: Fraud detection rules (block if suspicious activity)
     * - 150: Account status rules (block if suspended)
     * - 100: Required data availability rules
     * - 50: Brand preference rules
     * - 10: Optional enhancement rules
     * 
     * @return rule priority (higher numbers = higher priority, evaluated first)
     */
    default int getPriority() {
        return 0;
    }
} 