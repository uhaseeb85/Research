package com.bank.ivr.auth.rule.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.AuthenticationRule;
import org.springframework.stereotype.Component;

/**
 * Rule to determine if the authentication process is complete.
 * Authentication is considered complete when all required tokens have been successfully validated.
 */
@Component
public class FullAuthenticationCompletionRule implements AuthenticationRule {
    
    @Override
    public boolean evaluate(AuthenticationContext context, CustomerProfile customerProfile) {
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