package com.bank.ivr.auth.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

/**
 * Interface for brand-specific authentication configuration.
 * Allows different brands to have different token priorities, rules, and requirements.
 */
public interface BrandAuthConfiguration {
    
    // Static empty collections for performance
    List<String> EMPTY_RULE_LIST = Collections.emptyList();
    Map<String, Integer> EMPTY_PRIORITY_MAP = Collections.emptyMap();
    
    /**
     * Gets the brand code this configuration applies to.
     * 
     * @return the brand code (e.g., "PREMIUM_BANK", "COMMUNITY_BANK", "BUSINESS_BANK")
     */
    String getBrandCode();
    
    /**
     * Gets the token definitions for this brand with brand-specific priorities.
     * 
     * @return list of token definitions ordered by priority
     */
    List<AuthTokenDefinition> getTokenDefinitions();
    
    /**
     * Gets the maximum overall attempts allowed for this brand.
     * 
     * @return maximum overall attempts
     */
    int getMaxOverallAttempts();
    
    /**
     * Gets brand-specific token attempt limits.
     * This overrides the default max attempts in token definitions if specified.
     * 
     * @return map of token name to max attempts
     */
    Map<String, Integer> getBrandSpecificTokenAttempts();
    
    /**
     * Determines if concurrent token authentication is allowed for this brand.
     * 
     * @return true if multiple tokens can be accepted simultaneously
     */
    boolean isConcurrentTokenAuthAllowed();
    
    /**
     * Gets brand-specific messages or prompts.
     * 
     * @return map of message keys to brand-specific text
     */
    Map<String, String> getBrandMessages();
    
    /**
     * Gets the priority of this configuration (used when multiple configs match).
     * Higher numbers indicate higher priority.
     * 
     * @return configuration priority
     */
    default int getPriority() {
        return 0;
    }
    
    // NEW RULE CONFIGURATION METHODS
    
    /**
     * Gets the list of token selection rules that apply to this brand.
     * Rules are identified by their Spring bean names.
     * 
     * @return list of token selection rule bean names
     */
    default List<String> getApplicableTokenSelectionRules() {
        return EMPTY_RULE_LIST; // Static empty list for performance
    }
    
    /**
     * Gets the list of eligibility rules that apply to this brand.
     * Rules are identified by their Spring bean names.
     * 
     * @return list of eligibility rule bean names
     */
    default List<String> getApplicableEligibilityRules() {
        return EMPTY_RULE_LIST; // Static empty list for performance
    }
    
    /**
     * Gets the list of post-validation rules that apply to this brand.
     * Rules are identified by their Spring bean names.
     * 
     * @return list of post-validation rule bean names
     */
    default List<String> getApplicablePostValidationRules() {
        return EMPTY_RULE_LIST; // Static empty list for performance
    }
    
    /**
     * Gets brand-specific rule priorities.
     * This allows the same rule to have different execution order across brands.
     * 
     * @return map of rule bean name to priority (higher numbers execute first)
     */
    default Map<String, Integer> getRulePriorities() {
        return EMPTY_PRIORITY_MAP; // Static empty map for performance
    }
    
    /**
     * Checks if a specific rule is enabled for this brand.
     * 
     * @param ruleName the rule bean name
     * @return true if the rule is enabled for this brand
     */
    default boolean isRuleEnabled(String ruleName) {
        return getApplicableTokenSelectionRules().contains(ruleName) ||
               getApplicableEligibilityRules().contains(ruleName) ||
               getApplicablePostValidationRules().contains(ruleName);
    }
} 