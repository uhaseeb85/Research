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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for building brand-aware authentication responses based on context state.
 */
@Service
public class AuthenticationResponseService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationResponseService.class);
    
    private final AuthenticationContextService contextService;
    private final BrandAuthConfigurationService brandConfigService;
    private final BrandFailurePolicyService failurePolicyService;
    
    @Autowired
    public AuthenticationResponseService(
            AuthenticationContextService contextService,
            BrandAuthConfigurationService brandConfigService,
            BrandFailurePolicyService failurePolicyService) {
        this.contextService = contextService;
        this.brandConfigService = brandConfigService;
        this.failurePolicyService = failurePolicyService;
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
        if (completionRule.isAuthenticationComplete(context, customerProfile)) {
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
                    .failedTokens(context.getFailedTokens())
                    .build();
        }
        
        // Check for overall attempts exhaustion
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
                    .failedTokens(context.getFailedTokens())
                    .build();
        }
        
        // Check brand-specific failure policy
        if (failurePolicyService.shouldFailAuthentication(context, customerProfile, brand)) {
            context.setCurrentStatus(AuthStatus.FAILED);
            contextService.deleteContext(context.getAttemptId());
            
            // Use brand-specific failure message based on policy
            String failureMessage = brandConfigService.getBrandMessage(brand, "policy_failure");
            if (failureMessage == null) {
                failureMessage = brandConfigService.getBrandMessage(brand, "failure");
                if (failureMessage == null) {
                    failureMessage = "Authentication failed. Unable to verify your identity with available methods.";
                }
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(failureMessage)
                    .failedTokens(context.getFailedTokens())
                    .authenticatedTokens(context.getAuthenticatedTokens())
                    .build();
        }
        
        // Determine next token to ask using brand-specific token definitions and failure policy
        AuthTokenDefinition nextToken = determineNextToken(context, brandTokenDefinitions);
        if (nextToken == null) {
            // Try to get alternative token based on failure policy
            nextToken = failurePolicyService.getNextAlternativeToken(context, brand, brandTokenDefinitions);
            
            if (nextToken == null) {
                // Check if partial authentication is allowed
                if (failurePolicyService.isPartialAuthenticationAllowed(context, brand)) {
                    context.setCurrentStatus(AuthStatus.AUTHENTICATED);
                    contextService.deleteContext(context.getAttemptId());
                    
                    String partialSuccessMessage = brandConfigService.getBrandMessage(brand, "partial_success");
                    if (partialSuccessMessage == null) {
                        partialSuccessMessage = "Partial authentication successful. Limited access granted.";
                    }
                    
                    return AuthenticationResponse.builder()
                            .attemptId(context.getAttemptId())
                            .status(AuthStatus.AUTHENTICATED)
                            .message(partialSuccessMessage)
                            .authenticatedTokens(context.getAuthenticatedTokens())
                            .failedTokens(context.getFailedTokens())
                            .build();
                }
                
                // No alternatives and no partial auth - fail
                context.setCurrentStatus(AuthStatus.FAILED);
                contextService.deleteContext(context.getAttemptId());
                
                String noMethodsMessage = brandConfigService.getBrandMessage(brand, "no_methods");
                if (noMethodsMessage == null) {
                    noMethodsMessage = "No available authentication methods.";
                }
                
                return AuthenticationResponse.builder()
                        .attemptId(context.getAttemptId())
                        .status(AuthStatus.FAILED)
                        .message(noMethodsMessage)
                        .failedTokens(context.getFailedTokens())
                        .authenticatedTokens(context.getAuthenticatedTokens())
                        .build();
            }
        }
        
        // Track that this token was asked in this attempt
        context.setLastAskedToken(nextToken.getName());
        context.addAskedToken(nextToken.getName());
        
        logger.debug("Token {} has been marked as asked for attempt {}. AskedTokens now: {}", 
                    nextToken.getName(), context.getAttemptId(), context.getAskedTokens());
        
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

                .authenticatedTokens(context.getAuthenticatedTokens())
                .failedTokens(context.getFailedTokens())
                .build();
    }
    
    /**
     * Determines the next token to ask for using brand-specific token definitions.
     * Now uses smart re-asking logic: can re-ask tokens if user didn't provide them,
     * but won't re-ask tokens that user provided but failed validation.
     */
    private AuthTokenDefinition determineNextToken(AuthenticationContext context, List<AuthTokenDefinition> brandTokenDefinitions) {
        AuthTokenDefinition bestToken = null;
        int highestPriority = -1;
        
        logger.debug("Determining next token for attempt: {}, askedTokens: {}, authenticatedTokens: {}, failedTokens: {}, askedTokensWithValidationFailure: {}", 
                    context.getAttemptId(), context.getAskedTokens(), context.getAuthenticatedTokens(), 
                    context.getFailedTokens(), context.getAskedTokensWithValidationFailure());
        
        // Loop through all token definitions to find the best one
        for (AuthTokenDefinition token : brandTokenDefinitions) {
            // Check if this token is eligible
            if (!context.getEligibleTokens().contains(token.getName())) {
                logger.debug("Skipping token {} - not eligible", token.getName());
                continue; // Skip this token
            }
            
            // Check if this token is already authenticated
            if (context.isTokenAuthenticated(token.getName())) {
                logger.debug("Skipping token {} - already authenticated", token.getName());
                continue; // Skip this token
            }
            
            // Check if this token has failed
            if (context.isTokenFailed(token.getName())) {
                logger.debug("Skipping token {} - failed", token.getName());
                continue; // Skip this token
            }
            
            // Check if this token has remaining attempts
            if (!context.hasRemainingAttemptsForToken(token.getName())) {
                logger.debug("Skipping token {} - no remaining attempts", token.getName());
                continue; // Skip this token
            }
            
            // NEW SMART RE-ASKING LOGIC: Use canReAskToken instead of isTokenAlreadyAsked
            if (!context.canReAskToken(token.getName())) {
                logger.debug("Skipping token {} - smart re-asking logic prevents re-asking " +
                           "(user provided this token but validation failed)", token.getName());
                continue; // Skip this token
            }
            
            // This token is valid, check if it has higher priority
            if (token.getPriority() > highestPriority) {
                bestToken = token;
                highestPriority = token.getPriority();
                logger.debug("Token {} is current best choice with priority {} (can re-ask: {})", 
                           token.getName(), token.getPriority(), context.canReAskToken(token.getName()));
            }
        }
        
        if (bestToken != null) {
            logger.debug("Selected token {} with priority {} for attempt {} (validation failures: {})", 
                        bestToken.getName(), bestToken.getPriority(), context.getAttemptId(),
                        context.getAskedTokenValidationFailureCount(bestToken.getName()));
        } else {
            logger.debug("No valid token found for attempt {}", context.getAttemptId());
        }
        
        return bestToken; // Will be null if no valid token found
    }
    
    /**
     * Determines secondary tokens that can be accepted using brand-specific definitions.
     * Now applies smart re-asking logic: tokens that failed validation after being specifically 
     * provided are excluded from secondary options.
     */
    private List<AuthTokenDefinition> determineSecondaryTokens(AuthenticationContext context, 
                                                              AuthTokenDefinition primaryToken,
                                                              List<AuthTokenDefinition> brandTokenDefinitions) {
        List<AuthTokenDefinition> secondaryTokens = new ArrayList<>();
        
        // Loop through all token definitions to find valid secondary tokens
        for (AuthTokenDefinition token : brandTokenDefinitions) {
            // Skip if not eligible
            if (!context.getEligibleTokens().contains(token.getName())) {
                continue;
            }
            
            // Skip if already authenticated
            if (context.isTokenAuthenticated(token.getName())) {
                continue;
            }
            
            // Skip if failed
            if (context.isTokenFailed(token.getName())) {
                continue;
            }
            
            // Skip if no remaining attempts
            if (!context.hasRemainingAttemptsForToken(token.getName())) {
                continue;
            }
            
            // Skip if this is the primary token
            if (token.getName().equals(primaryToken.getName())) {
                continue;
            }
            
            // APPLY SMART RE-ASKING LOGIC: Skip tokens that user provided but failed validation
            if (!context.canReAskToken(token.getName())) {
                continue;
            }
            
            // This token is valid as a secondary token
            secondaryTokens.add(token);
        }
        
        return secondaryTokens;
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