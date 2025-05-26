package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.TokenSelectionRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if the authentication process is complete.
 * Authentication is considered complete when at least one eligible token has been successfully validated.
 */
@Component
public class FullAuthenticationCompletionRule implements TokenSelectionRule {
    
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        // This rule doesn't select tokens, it just checks completion
        return null;
    }
    
    @Override
    public boolean isApplicable(AuthenticationContext context, CustomerProfile customerProfile) {
        // Always applicable for completion checking
        return true;
    }
    
    /**
     * Checks if authentication is complete.
     */
    public boolean isAuthenticationComplete(AuthenticationContext context, CustomerProfile customerProfile) {
        // Authentication is complete when at least one token has been successfully authenticated
        return !context.getAuthenticatedTokens().isEmpty();
    }
    
    @Override
    public String getRuleName() {
        return "FULL_AUTHENTICATION_COMPLETION";
    }
    
    @Override
    public int getPriority() {
        return 1000; // Highest priority - this determines completion
    }
} 