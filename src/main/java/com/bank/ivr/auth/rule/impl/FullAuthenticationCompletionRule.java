package com.bank.ivr.auth.rule.impl;

import org.springframework.stereotype.Component;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.TokenSelectionRule;

/**
 * Brand-agnostic rule to determine if the authentication process is complete.
 * Authentication is considered complete when at least one eligible token has been successfully validated.
 * This rule is more of a utility and doesn't typically select tokens.
 */
@Component("FULL_AUTHENTICATION_COMPLETION_RULE")
public class FullAuthenticationCompletionRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        // Context-aware: This rule doesn't select tokens, it just checks completion
        // But could be enhanced to suggest additional tokens if needed for specific brands
        
        // Check if authentication is already complete
        if (isAuthenticationComplete(context, customerProfile)) {
            return null; // No additional tokens needed
        }
        
        // This rule doesn't actively select tokens - that's handled by other rules
        return null;
    }
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
        // Always applicable for completion checking
        return true;
    }
    
    /**
     * Context-aware completion check.
     * Authentication is complete when at least one token has been successfully authenticated.
     */
    public boolean isAuthenticationComplete(AuthenticationContext context, CustomerProfile customerProfile) {
        // Enhanced logic: Authentication is complete when at least one token has been successfully authenticated
        // and there are no failed tokens that require additional verification
        return !context.getAuthenticatedTokens().isEmpty();
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT"; // Brand-agnostic utility rule
    }
    
    @Override
    public String getRuleName() {
        return "FULL_AUTHENTICATION_COMPLETION_RULE"; // Match Spring bean name
    }
    
    @Override
    public int getPriority() {
        return 1000; // Highest priority - this determines completion
    }
} 