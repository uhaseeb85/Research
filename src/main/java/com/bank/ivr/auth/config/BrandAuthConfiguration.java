package com.bank.ivr.auth.config;import com.bank.ivr.auth.model.domain.AuthTokenDefinition;import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;import com.bank.ivr.auth.model.domain.TokenRetryStrategy;import java.util.List;import java.util.Map;

/**
 * Interface for brand-specific authentication configuration.
 * Allows different brands to have different token priorities, rules, and requirements.
 */
public interface BrandAuthConfiguration {
    
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
     * Gets the required tokens for full authentication for this brand.
     * 
     * @return list of required token names
     */
    List<String> getRequiredTokens();
    
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
    
        /**     * Gets brand-specific messages or prompts.     *      * @return map of message keys to brand-specific text     */    Map<String, String> getBrandMessages();        /**     * Gets brand-specific retry strategies for tokens.     *      * @return map of token name to retry strategy     */    Map<String, TokenRetryStrategy> getTokenRetryStrategies();        /**     * Gets the global retry policy for this brand.     * Controls overall retry behavior across all tokens.     *      * @return global retry configuration     */    BrandGlobalRetryPolicy getGlobalRetryPolicy();
    
    /**
     * Gets the priority of this configuration (used when multiple configs match).
     * Higher numbers indicate higher priority.
     * 
     * @return configuration priority
     */
    default int getPriority() {
        return 0;
    }
} 