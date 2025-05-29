package com.bank.ivr.auth.rule.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.rule.PostValidationRule;

/**
 * Post-validation rule that determines if additional tokens should be requested
 * after successful validation based on trust levels, phone matching, and customer profile attributes.
 * 
 * This rule implements common scenarios where additional authentication is needed:
 * - Low trust level (RED) requires additional verification
 * - Phone number not matched or multiple matches increases risk
 * - High-value customer accounts need extra security
 * - Certain customer profile attributes trigger additional checks
 */
@Component
public class TrustBasedAdditionalTokenRule implements PostValidationRule {
    
    @Override
    public TokenValidationResult evaluatePostValidation(String validatedToken, 
                                                       AuthenticationContext context, 
                                                       CustomerProfile customerProfile) {
        
        TrustLevelInfo trustInfo = context.getTrustLevelInfo();
        if (trustInfo == null) {
            // No trust info available, proceed with standard authentication
            return TokenValidationResult.success();
        }
        
        // Scenario 1: Low trust level (RED) requires additional verification
        if (trustInfo.isLowTrust()) {
            return handleLowTrustScenario(validatedToken, context, customerProfile, trustInfo);
        }
        
        // Scenario 2: High trust but phone number issues
        if (trustInfo.isHighTrust() && hasPhoneNumberIssues(trustInfo)) {
            return handlePhoneNumberIssues(validatedToken, context, customerProfile, trustInfo);
        }
        
        // Scenario 3: High-value customer account requires additional security
        if (isHighValueCustomer(customerProfile)) {
            return handleHighValueCustomer(validatedToken, context, customerProfile);
        }
        
        // Scenario 4: Customer profile attributes trigger additional checks
        if (hasRiskIndicators(customerProfile)) {
            return handleRiskIndicators(validatedToken, context, customerProfile);
        }
        
        // No additional tokens required
        return TokenValidationResult.success();
    }
    
    /**
     * Handles low trust scenarios - always requires additional verification.
     */
    private TokenValidationResult handleLowTrustScenario(String validatedToken, 
                                                        AuthenticationContext context, 
                                                        CustomerProfile customerProfile,
                                                        TrustLevelInfo trustInfo) {
        
        // For low trust, we need additional verification regardless of what token was validated
        List<String> additionalTokens = determineAdditionalTokensForLowTrust(validatedToken, context, customerProfile);
        
        if (!additionalTokens.isEmpty()) {
            String reason = String.format("Low trust level detected (RED) with phone match status: %s. Additional verification required for security.", 
                                        trustInfo.getPhoneMatchStatus());
            return TokenValidationResult.successWithAdditionalTokensRequired(additionalTokens, reason, "HIGH");
        }
        
        return TokenValidationResult.success();
    }
    
    /**
     * Handles phone number matching issues.
     */
    private TokenValidationResult handlePhoneNumberIssues(String validatedToken, 
                                                         AuthenticationContext context, 
                                                         CustomerProfile customerProfile,
                                                         TrustLevelInfo trustInfo) {
        
        if (trustInfo.hasMultiplePhoneMatches()) {
            // Phone matches multiple customers - need to verify identity more thoroughly
            List<String> additionalTokens = Arrays.asList("SSN_FULL", "DATE_OF_BIRTH");
            String reason = String.format("Phone number matches %d customer accounts. Additional verification required to confirm identity.", 
                                        trustInfo.getMatchedSsnCount());
            return TokenValidationResult.successWithAdditionalTokensRequired(additionalTokens, reason, "MEDIUM");
        }
        
        if (!trustInfo.hasPhoneMatch()) {
            // Phone not in our records - moderate additional verification
            List<String> additionalTokens = Arrays.asList("SSN_LAST_4", "ACCOUNT_NUMBER");
            String reason = "Phone number not found in our records. Additional verification required.";
            return TokenValidationResult.successWithAdditionalTokensRequired(additionalTokens, reason, "MEDIUM");
        }
        
        return TokenValidationResult.success();
    }
    
    /**
     * Handles high-value customer scenarios.
     */
    private TokenValidationResult handleHighValueCustomer(String validatedToken, 
                                                         AuthenticationContext context, 
                                                         CustomerProfile customerProfile) {
        
        // High-value customers get additional security regardless of trust level
        if (!"SSN_FULL".equals(validatedToken) && !"DEBIT_CARD_PIN".equals(validatedToken)) {
            List<String> additionalTokens = Arrays.asList("DEBIT_CARD_PIN", "DATE_OF_BIRTH");
            String reason = "High-value customer account detected. Enhanced security verification required.";
            return TokenValidationResult.successWithAdditionalTokensRequired(additionalTokens, reason, "MEDIUM");
        }
        
        return TokenValidationResult.success();
    }
    
    /**
     * Handles customer profile risk indicators.
     */
    private TokenValidationResult handleRiskIndicators(String validatedToken, 
                                                      AuthenticationContext context, 
                                                      CustomerProfile customerProfile) {
        
        // Check for recent suspicious activity or account changes
        if (hasRecentAccountChanges(customerProfile) || hasSuspiciousActivity(customerProfile)) {
            List<String> additionalTokens = Arrays.asList("SSN_LAST_4", "DEBIT_CARD_PIN");
            String reason = "Recent account activity or changes detected. Additional verification required for security.";
            return TokenValidationResult.successWithAdditionalTokensRequired(additionalTokens, reason, "MEDIUM");
        }
        
        return TokenValidationResult.success();
    }
    
    /**
     * Determines additional tokens needed for low trust scenarios.
     */
    private List<String> determineAdditionalTokensForLowTrust(String validatedToken, 
                                                             AuthenticationContext context, 
                                                             CustomerProfile customerProfile) {
        
        // For low trust, we need strong additional verification
        if ("SSN_LAST_4".equals(validatedToken)) {
            // If they provided SSN last 4, ask for full SSN and PIN
            return Arrays.asList("SSN_FULL", "DEBIT_CARD_PIN");
        } else if ("DEBIT_CARD_PIN".equals(validatedToken)) {
            // If they provided PIN, ask for full SSN and DOB
            return Arrays.asList("SSN_FULL", "DATE_OF_BIRTH");
        } else if ("SSN_FULL".equals(validatedToken)) {
            // If they provided full SSN, ask for PIN and DOB
            return Arrays.asList("DEBIT_CARD_PIN", "DATE_OF_BIRTH");
        } else {
            // For any other token, ask for SSN and PIN
            return Arrays.asList("SSN_FULL", "DEBIT_CARD_PIN");
        }
    }
    
    /**
     * Checks if there are phone number matching issues.
     */
    private boolean hasPhoneNumberIssues(TrustLevelInfo trustInfo) {
        return !trustInfo.hasPhoneMatch() || trustInfo.hasMultiplePhoneMatches();
    }
    
    /**
     * Determines if customer is high-value based on profile attributes.
     */
    private boolean isHighValueCustomer(CustomerProfile customerProfile) {
        // Example criteria for high-value customers
        // Since CustomerProfile doesn't have accountType/tier/balance fields,
        // we'll use available fields to determine high-value status
        return customerProfile.getEmployeeId() != null || // Employee accounts are high-value
               customerProfile.getEmail() != null && customerProfile.getEmail().contains("@premium") || // Premium email domains
               customerProfile.getFullName() != null && customerProfile.getFullName().length() > 15; // Longer names might indicate business accounts
    }
    
    /**
     * Checks for risk indicators in customer profile.
     */
    private boolean hasRiskIndicators(CustomerProfile customerProfile) {
        return hasRecentAccountChanges(customerProfile) || hasSuspiciousActivity(customerProfile);
    }
    
    /**
     * Checks for recent account changes.
     */
    private boolean hasRecentAccountChanges(CustomerProfile customerProfile) {
        // This would typically check timestamps of recent changes
        // For demo purposes, we'll use available fields to simulate change detection
        // In a real implementation, you would have timestamp fields for tracking changes
        return customerProfile.getAddress() != null && customerProfile.getAddress().contains("New") ||
               customerProfile.getEmail() != null && customerProfile.getEmail().contains("temp") ||
               customerProfile.getPhoneNumber() != null && customerProfile.getPhoneNumber().startsWith("+1555"); // Temporary phone numbers
    }
    
    /**
     * Checks for suspicious activity indicators.
     */
    private boolean hasSuspiciousActivity(CustomerProfile customerProfile) {
        // This would typically check fraud indicators or recent failed attempts
        // For demo purposes, we'll check account status
        return "FLAGGED".equals(customerProfile.getAccountStatus()) ||
               "UNDER_REVIEW".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public boolean isApplicable(String validatedToken, 
                               AuthenticationContext context, 
                               CustomerProfile customerProfile) {
        
        // This rule applies to all brands and all tokens
        // It's a general trust-based rule that can be used across different scenarios
        return context.getTrustLevelInfo() != null && 
               ("ACTIVE".equals(customerProfile.getAccountStatus()) || 
                "FLAGGED".equals(customerProfile.getAccountStatus()) ||
                "UNDER_REVIEW".equals(customerProfile.getAccountStatus()));
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Applies to all brands
    }
    
    @Override
    public String getRuleName() {
        return "TRUST_BASED_ADDITIONAL_TOKEN_RULE";
    }
    
    @Override
    public int getPriority() {
        return 100; // Medium priority - brand-specific rules should have higher priority
    }
    
    @Override
    public String getConditionDescription() {
        return "Evaluates trust level, phone matching status, and customer profile attributes " +
               "to determine if additional authentication tokens are required after successful validation. " +
               "Handles scenarios including low trust levels, phone number issues, high-value customers, " +
               "and risk indicators.";
    }
    
    @Override
    public List<String> getApplicableTokens() {
        // This rule can be triggered by any token validation
        return Arrays.asList("SSN_LAST_4", "SSN_FULL", "DEBIT_CARD_PIN", "DATE_OF_BIRTH", "ACCOUNT_NUMBER");
    }
} 