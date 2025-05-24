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
 * Service responsible for building brand-aware authentication responses based on context state.
 */
@Service
public class AuthenticationResponseService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationResponseService.class);
    
    private final AuthenticationContextService contextService;
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public AuthenticationResponseService(
            AuthenticationContextService contextService,
            BrandAuthConfigurationService brandConfigService) {
        this.contextService = contextService;
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Builds the response based on the current context state with full brand awareness.
     */
    public AuthenticationResponse buildResponse(AuthenticationContext context, CustomerProfile customerProfile, String brand) {
        logger.debug("Building brand-aware response for attempt: {}, brand: {}", context.getAttemptId(), brand);
        
        // Get brand-specific token definitions
        List<AuthTokenDefinition> brandTokenDefinitions = brandConfigService.getTokenDefinitionsForBrand(brand);
        
        // Check for authentication completion
        FullAuthenticationCompletionRule completionRule = new FullAuthenticationCompletionRule();
        if (completionRule.evaluate(context, customerProfile)) {
            context.setCurrentStatus(AuthStatus.AUTHENTICATED);
            contextService.deleteContext(context.getAttemptId());
            
            // Use brand-specific success message
            String successMessage = brandConfigService.getBrandMessage(brand, "success");
            if (successMessage == null) {
                successMessage = "Authentication successful. Welcome!";
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.AUTHENTICATED)
                    .message(successMessage)
                    .authenticatedTokens(context.getAuthenticatedTokens())
                    .build();
        }
        
        // Check for failure conditions
        if (context.getOverallAttemptsRemaining() <= 0) {
            context.setCurrentStatus(AuthStatus.FAILED);
            contextService.deleteContext(context.getAttemptId());
            
            // Use brand-specific failure message
            String failureMessage = brandConfigService.getBrandMessage(brand, "failure");
            if (failureMessage == null) {
                failureMessage = "Authentication failed. Too many incorrect attempts.";
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(failureMessage)
                    .build();
        }
        
        // Determine next token to ask using brand-specific token definitions
        AuthTokenDefinition nextToken = determineNextToken(context, brandTokenDefinitions);
        if (nextToken == null) {
            context.setCurrentStatus(AuthStatus.FAILED);
            contextService.deleteContext(context.getAttemptId());
            
            // Use brand-specific no methods message
            String noMethodsMessage = brandConfigService.getBrandMessage(brand, "no_methods");
            if (noMethodsMessage == null) {
                noMethodsMessage = "No available authentication methods.";
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(noMethodsMessage)
                    .build();
        }
        
        context.setLastAskedToken(nextToken.getName());
        
        // Determine secondary tokens using brand-specific definitions
        List<AuthTokenDefinition> secondaryTokens = determineSecondaryTokens(context, nextToken, brandTokenDefinitions);
        
        // Build remaining attempts map
        Map<String, Integer> remainingAttempts = new HashMap<>(context.getTokenAttemptsRemaining());
        remainingAttempts.put("OVERALL", context.getOverallAttemptsRemaining());
        
        return AuthenticationResponse.builder()
                .attemptId(context.getAttemptId())
                .status(context.getAuthenticatedTokens().isEmpty() ? 
                       AuthStatus.PENDING_PRIMARY_TOKEN : AuthStatus.PENDING_MORE_TOKENS)
                .message(buildMessage(nextToken, context, brand))
                .primaryTokenToAsk(nextToken)
                .secondaryTokensAccepted(secondaryTokens)
                .remainingAttempts(remainingAttempts)
                .requiredTokensRemaining(context.getRequiredTokensForFullAuth())
                .authenticatedTokens(context.getAuthenticatedTokens())
                .build();
    }
    
    /**
     * Determines the next token to ask for using brand-specific token definitions.
     */
    private AuthTokenDefinition determineNextToken(AuthenticationContext context, List<AuthTokenDefinition> brandTokenDefinitions) {
        return brandTokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .max(Comparator.comparingInt(AuthTokenDefinition::getPriority))
                .orElse(null);
    }
    
    /**
     * Determines secondary tokens that can be accepted using brand-specific definitions.
     */
    private List<AuthTokenDefinition> determineSecondaryTokens(AuthenticationContext context, 
                                                              AuthTokenDefinition primaryToken,
                                                              List<AuthTokenDefinition> brandTokenDefinitions) {
        return brandTokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .filter(token -> !token.getName().equals(primaryToken.getName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Builds an appropriate brand-aware message for the customer.
     */
    private String buildMessage(AuthTokenDefinition nextToken, AuthenticationContext context, String brand) {
        if (context.getAuthenticatedTokens().isEmpty()) {
            // Use brand-specific primary prompt if available
            String primaryPrompt = brandConfigService.getBrandMessage(brand, "primary_prompt");
            if (primaryPrompt != null) {
                return primaryPrompt.replace("{token_description}", nextToken.getDescription());
            }
            return "Please provide your " + nextToken.getDescription() + ".";
        } else {
            // Use brand-specific secondary prompt if available
            String secondaryPrompt = brandConfigService.getBrandMessage(brand, "secondary_prompt");
            if (secondaryPrompt != null) {
                return secondaryPrompt.replace("{token_description}", nextToken.getDescription());
            }
            return "Thank you. Now please provide your " + nextToken.getDescription() + ".";
        }
    }
} 