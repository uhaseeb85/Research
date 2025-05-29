package com.bank.ivr.auth.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.DnisConfiguration;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.rule.impl.FullAuthenticationCompletionRule;

/**
 * Service responsible for building authentication responses based on context state.
 * Now supports brand-aware responses and smart re-asking logic.
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
        
        // Determine next token to ask using brand-specific token definitions
        AuthTokenDefinition nextToken = determineNextToken(context, brandTokenDefinitions);
        if (nextToken == null) {
            // Try to get alternative token based on failure policy
            nextToken = determineNextToken(context, brandTokenDefinitions);
            
            if (nextToken == null) {
                // Check if partial authentication is allowed
                if (isPartialAuthenticationAllowed(context, brand)) {
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

    /**
     * Builds the response based on the current context state with full brand awareness and DNIS configuration.
     */
    public AuthenticationResponse buildResponseWithDnis(AuthenticationContext context, CustomerProfile customerProfile, 
                                                       String brand, DnisConfiguration dnisConfig) {
        logger.debug("Building DNIS-aware response for attempt: {}, brand: {}, dnis: {}", 
                    context.getAttemptId(), brand, dnisConfig.getDnis());
        
        // Get brand-specific token definitions filtered by DNIS configuration
        List<AuthTokenDefinition> brandTokenDefinitions = brandConfigService.getTokenDefinitionsForBrand(brand);
        List<AuthTokenDefinition> dnisFilteredTokens = filterTokensByDnis(brandTokenDefinitions, dnisConfig);
        
        // Check for authentication completion
        FullAuthenticationCompletionRule completionRule = new FullAuthenticationCompletionRule();
        
        // Check if DNIS requires multi-factor authentication
        boolean requiresMultiFactor = dnisConfig.isRequireMultiFactorAuth();
        if (requiresMultiFactor && context.getAuthenticatedTokens().size() < 2) {
            logger.debug("DNIS requires multi-factor authentication, current tokens: {}", 
                        context.getAuthenticatedTokens().size());
        }
        
        if (completionRule.isAuthenticationComplete(context, customerProfile) && 
            (!requiresMultiFactor || context.getAuthenticatedTokens().size() >= 2)) {
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
        
        // Check for overall attempts exhaustion using DNIS-specific limits
        int maxAttempts = dnisConfig.getMaxAuthenticationAttempts();
        if (context.getOverallAttemptsRemaining() <= 0 || 
            (maxAttempts > 0 && context.getOverallAttemptsRemaining() > maxAttempts)) {
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
        
        // Determine next token using DNIS-filtered token definitions
        AuthTokenDefinition nextToken = determineNextToken(context, dnisFilteredTokens);
        if (nextToken == null) {
            // Try alternative tokens based on DNIS allowance
            if (dnisConfig.isAllowAlternativeTokens()) {
                nextToken = determineNextToken(context, dnisFilteredTokens);
            }
            
            if (nextToken == null) {
                // Check if partial authentication is allowed
                if (isPartialAuthenticationAllowed(context, brand)) {
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
                
                String noMethodsMessage = buildDnisFailureMessage(brand, dnisConfig);
                
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
        
        logger.debug("Token {} has been marked as asked for attempt {} (DNIS: {}). AskedTokens now: {}", 
                    nextToken.getName(), context.getAttemptId(), dnisConfig.getDnis(), context.getAskedTokens());
        
        // Determine secondary tokens using DNIS-filtered definitions
        List<AuthTokenDefinition> secondaryTokens = determineSecondaryTokens(context, nextToken, dnisFilteredTokens);
        
        // Build remaining attempts map
        Map<String, Integer> remainingAttempts = new HashMap<>(context.getTokenAttemptsRemaining());
        remainingAttempts.put("OVERALL", context.getOverallAttemptsRemaining());
        
        return AuthenticationResponse.builder()
                .attemptId(context.getAttemptId())
                .status(context.getAuthenticatedTokens().isEmpty() ? 
                       AuthStatus.PENDING_PRIMARY_TOKEN : AuthStatus.PENDING_MORE_TOKENS)
                .message(buildMessageWithDnis(nextToken, context, brand, dnisConfig))
                .primaryTokenToAsk(nextToken)
                .secondaryTokensAccepted(secondaryTokens)
                .remainingAttempts(remainingAttempts)
                .authenticatedTokens(context.getAuthenticatedTokens())
                .failedTokens(context.getFailedTokens())
                .build();
    }
    
    /**
     * Filters token definitions based on DNIS configuration.
     */
    private List<AuthTokenDefinition> filterTokensByDnis(List<AuthTokenDefinition> tokenDefinitions, DnisConfiguration dnisConfig) {
        List<AuthTokenDefinition> filteredTokens = new ArrayList<>();
        
        for (AuthTokenDefinition token : tokenDefinitions) {
            if (isTokenAllowedByDnis(token.getName(), dnisConfig)) {
                filteredTokens.add(token);
            } else {
                logger.debug("Token '{}' filtered out by DNIS configuration '{}'", token.getName(), dnisConfig.getDnis());
            }
        }
        
        return filteredTokens;
    }
    
    /**
     * Checks if a token is allowed by DNIS configuration.
     */
    private boolean isTokenAllowedByDnis(String tokenName, DnisConfiguration dnisConfig) {
        switch (tokenName.toUpperCase()) {
            case "SSN":
            case "SSN_LAST_4":
            case "SSN_FULL":
                return dnisConfig.isAllowSsnAuthentication();
            case "DEBIT_CARD_PIN":
            case "PIN":
                return dnisConfig.isAllowPinAuthentication();
            case "DATE_OF_BIRTH":
                return dnisConfig.isAllowDateOfBirthAuthentication();
            case "MOTHER_MAIDEN_NAME":
                return dnisConfig.isAllowMotherMaidenNameAuthentication();
            case "ACCOUNT_NUMBER":
                return dnisConfig.isAllowAccountNumberAuthentication();
            default:
                return true; // Allow unknown tokens by default
        }
    }
    
    /**
     * Builds failure message considering DNIS configuration.
     */
    private String buildDnisFailureMessage(String brand, DnisConfiguration dnisConfig) {
        String failureMessage = brandConfigService.getBrandMessage(brand, "no_methods");
        if (failureMessage == null) {
            if (dnisConfig.isEnableStrictValidation()) {
                failureMessage = "Authentication failed. Strict security policy applied.";
            } else {
                failureMessage = "No available authentication methods.";
            }
        }
        return failureMessage;
    }
    
    /**
     * Builds message with DNIS considerations.
     */
    private String buildMessageWithDnis(AuthTokenDefinition nextToken, AuthenticationContext context, 
                                       String brand, DnisConfiguration dnisConfig) {
        String message = buildMessage(nextToken, context, brand);
        
        // Add DNIS-specific messaging if strict validation is enabled
        if (dnisConfig.isEnableStrictValidation()) {
            message += " (Enhanced security applied)";
        }
        
        // Add multi-factor requirement message if needed
        if (dnisConfig.isRequireMultiFactorAuth() && context.getAuthenticatedTokens().size() == 1) {
            message += " Additional verification required.";
        }
        
        return message;
    }

    /**
     * Checks if partial authentication is allowed based on the context and brand.
     * This method determines if the authentication can succeed with fewer tokens
     * than normally required based on brand policies and current authentication state.
     */
    private boolean isPartialAuthenticationAllowed(AuthenticationContext context, String brand) {
        // Check if the brand supports partial authentication
        int authenticatedTokenCount = context.getAuthenticatedTokens().size();
        
        // Minimum requirement: at least one token must be authenticated
        if (authenticatedTokenCount == 0) {
            logger.debug("Partial authentication denied - no tokens authenticated for brand: {}", brand);
            return false;
        }
        
        // Brand-specific logic for partial authentication
        switch (brand) {
            case "PREMIUM_BANK":
                // Premium bank requires at least 1 high-priority token
                return authenticatedTokenCount >= 1;
                
            case "COMMUNITY_BANK":
                // Community bank is more lenient, allows partial with 1 token
                return authenticatedTokenCount >= 1;
                
            case "TECH_BANK":
                // Tech bank requires at least 1 token but prefers 2
                return authenticatedTokenCount >= 1;
                
            default:
                // Default policy: allow partial authentication with at least 1 token
                boolean allowed = authenticatedTokenCount >= 1;
                logger.debug("Partial authentication for brand '{}': {} (authenticated tokens: {})", 
                           brand, allowed, authenticatedTokenCount);
                return allowed;
        }
    }
} 