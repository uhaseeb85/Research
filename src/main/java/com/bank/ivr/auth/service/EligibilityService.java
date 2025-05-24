package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.AuthenticationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for determining which authentication tokens a customer is eligible for.
 * Uses rule-based evaluation for flexible business logic.
 */
@Service
public class EligibilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(EligibilityService.class);
    
    private final List<AuthenticationRule> rules;
    
    @Autowired
    public EligibilityService(List<AuthenticationRule> rules) {
        this.rules = rules;
    }
    
    /**
     * Determines which tokens a customer is eligible for based on their profile.
     * Uses rule-based evaluation for flexible business logic.
     */
    public List<String> determineEligibleTokens(CustomerProfile customerProfile) {
        List<String> eligibleTokens = new ArrayList<>();
        
        logger.debug("Determining eligible tokens for customer: {}", customerProfile.getCustomerId());
        
        // Create a temporary context for rule evaluation (rules shouldn't need attemptId/sessionId for eligibility)
        // We'll use the rules to check eligibility, but only the customer profile based ones
        for (AuthenticationRule rule : rules) {
            try {
                // Skip the completion rule as it's not an eligibility rule
                if ("FULL_AUTHENTICATION_COMPLETION".equals(rule.getRuleName())) {
                    continue;
                }
                
                // For eligibility rules, we only need customer profile, not full authentication context
                // The current eligibility rules (SSN_ELIGIBILITY, DEBIT_CARD_PIN_ELIGIBILITY) don't actually use context
                boolean isEligible = rule.evaluate(null, customerProfile);
                
                if (isEligible) {
                    String tokenName = mapRuleNameToTokenName(rule.getRuleName());
                    if (tokenName != null) {
                        eligibleTokens.add(tokenName);
                        logger.debug("Customer eligible for {} authentication", tokenName);
                    }
                }
            } catch (Exception e) {
                // Log the error but continue with other rules
                logger.error("Error evaluating rule {}: {}", rule.getRuleName(), e.getMessage());
            }
        }
        
        // Fallback: if no rules worked, use the original hard-coded logic
        if (eligibleTokens.isEmpty()) {
            eligibleTokens = determineFallbackEligibleTokens(customerProfile);
        }
        
        logger.debug("Eligible tokens: {}", eligibleTokens);
        return eligibleTokens;
    }
    
    /**
     * Fallback logic for token eligibility when rules fail.
     */
    private List<String> determineFallbackEligibleTokens(CustomerProfile customerProfile) {
        logger.debug("Rule evaluation failed, falling back to hard-coded eligibility logic");
        
        List<String> eligibleTokens = new ArrayList<>();
        
        // Check SSN eligibility
        if (customerProfile.getSsn() != null && !customerProfile.getSsn().trim().isEmpty() 
            && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("SSN");
            logger.debug("Customer eligible for SSN authentication");
        }
        
        // Check Debit Card PIN eligibility
        if (customerProfile.getHashedPin() != null && !customerProfile.getHashedPin().trim().isEmpty() 
            && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("DEBIT_CARD_PIN");
            logger.debug("Customer eligible for DEBIT_CARD_PIN authentication");
        }
        
        // Check Date of Birth eligibility
        if (customerProfile.getDateOfBirth() != null && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("DATE_OF_BIRTH");
            logger.debug("Customer eligible for DATE_OF_BIRTH authentication");
        }
        
        // Check Mother's Maiden Name eligibility
        if (customerProfile.getMotherMaidenName() != null && !customerProfile.getMotherMaidenName().trim().isEmpty() 
            && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("MOTHER_MAIDEN_NAME");
            logger.debug("Customer eligible for MOTHER_MAIDEN_NAME authentication");
        }
        
        // Check Employee ID eligibility
        if (customerProfile.getEmployeeId() != null && !customerProfile.getEmployeeId().trim().isEmpty() 
            && "ACTIVE".equals(customerProfile.getAccountStatus())) {
            eligibleTokens.add("EMPLOYEE_ID");
            logger.debug("Customer eligible for EMPLOYEE_ID authentication");
        }
        
        return eligibleTokens;
    }
    
    /**
     * Maps rule name to token name for eligibility rules.
     */
    private String mapRuleNameToTokenName(String ruleName) {
        switch (ruleName) {
            case "SSN_ELIGIBILITY":
                return "SSN";
            case "DEBIT_CARD_PIN_ELIGIBILITY":
                return "DEBIT_CARD_PIN";
            case "DATE_OF_BIRTH_ELIGIBILITY":
                return "DATE_OF_BIRTH";
            case "MOTHER_MAIDEN_NAME_ELIGIBILITY":
                return "MOTHER_MAIDEN_NAME";
            case "EMPLOYEE_ID_ELIGIBILITY":
                return "EMPLOYEE_ID";
            default:
                return null;
        }
    }
} 