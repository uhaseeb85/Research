package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.rule.impl.FullAuthenticationCompletionRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for building authentication responses based on context state.
 */
@Service
public class AuthenticationResponseService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationResponseService.class);
    
    private final List<AuthTokenDefinition> tokenDefinitions;
    private final AuthenticationContextService contextService;
    
    @Autowired
    public AuthenticationResponseService(
            List<AuthTokenDefinition> tokenDefinitions,
            AuthenticationContextService contextService) {
        this.tokenDefinitions = tokenDefinitions;
        this.contextService = contextService;
    }
    
    /**
     * Builds the response based on the current context state.
     */
    public AuthenticationResponse buildResponse(AuthenticationContext context, CustomerProfile customerProfile) {
        // Check for authentication completion
        FullAuthenticationCompletionRule completionRule = new FullAuthenticationCompletionRule();
        if (completionRule.evaluate(context, customerProfile)) {
            context.setCurrentStatus(AuthStatus.AUTHENTICATED);
            contextService.deleteContext(context.getAttemptId());
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.AUTHENTICATED)
                    .message("Authentication successful. Welcome!")
                    .authenticatedTokens(context.getAuthenticatedTokens())
                    .build();
        }
        
        // Check for failure conditions
        if (context.getOverallAttemptsRemaining() <= 0) {
            context.setCurrentStatus(AuthStatus.FAILED);
            contextService.deleteContext(context.getAttemptId());
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("Authentication failed. Too many incorrect attempts.")
                    .build();
        }
        
        // Determine next token to ask
        AuthTokenDefinition nextToken = determineNextToken(context);
        if (nextToken == null) {
            context.setCurrentStatus(AuthStatus.FAILED);
            contextService.deleteContext(context.getAttemptId());
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("No available authentication methods.")
                    .build();
        }
        
        context.setLastAskedToken(nextToken.getName());
        
        // Determine secondary tokens
        List<AuthTokenDefinition> secondaryTokens = determineSecondaryTokens(context, nextToken);
        
        // Build remaining attempts map
        Map<String, Integer> remainingAttempts = new HashMap<>(context.getTokenAttemptsRemaining());
        remainingAttempts.put("OVERALL", context.getOverallAttemptsRemaining());
        
        return AuthenticationResponse.builder()
                .attemptId(context.getAttemptId())
                .status(context.getAuthenticatedTokens().isEmpty() ? 
                       AuthStatus.PENDING_PRIMARY_TOKEN : AuthStatus.PENDING_MORE_TOKENS)
                .message(buildMessage(nextToken, context))
                .primaryTokenToAsk(nextToken)
                .secondaryTokensAccepted(secondaryTokens)
                .remainingAttempts(remainingAttempts)
                .requiredTokensRemaining(context.getRequiredTokensForFullAuth())
                .authenticatedTokens(context.getAuthenticatedTokens())
                .build();
    }
    
    /**
     * Determines the next token to ask for.
     */
    private AuthTokenDefinition determineNextToken(AuthenticationContext context) {
        return tokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .max(Comparator.comparingInt(AuthTokenDefinition::getPriority))
                .orElse(null);
    }
    
    /**
     * Determines secondary tokens that can be accepted.
     */
    private List<AuthTokenDefinition> determineSecondaryTokens(AuthenticationContext context, 
                                                              AuthTokenDefinition primaryToken) {
        return tokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .filter(token -> !token.getName().equals(primaryToken.getName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Builds an appropriate message for the customer.
     */
    private String buildMessage(AuthTokenDefinition nextToken, AuthenticationContext context) {
        if (context.getAuthenticatedTokens().isEmpty()) {
            return "Please provide your " + nextToken.getDescription() + ".";
        } else {
            return "Thank you. Now please provide your " + nextToken.getDescription() + ".";
        }
    }
} 