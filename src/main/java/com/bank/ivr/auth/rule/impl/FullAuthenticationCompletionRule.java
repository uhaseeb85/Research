package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.TokenSelectionRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if the authentication process is complete.
 * Authentication is considered complete when all required tokens have been successfully validated.
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
        // Check if all required tokens have been authenticated
        return context.getRequiredTokensForFullAuth().isEmpty() 
               || context.getAuthenticatedTokens().containsAll(context.getRequiredTokensForFullAuth());
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