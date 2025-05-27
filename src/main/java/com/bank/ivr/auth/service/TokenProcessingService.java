package com.bank.ivr.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.ProvidedToken;

/**
 * Service responsible for processing and validating customer-provided tokens.
 * Now supports brand-aware token validation and post-validation rules.
 */
@Service
public class TokenProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenProcessingService.class);
    
    private final TokenValidationService tokenValidationService;
    
    @Autowired
    public TokenProcessingService(TokenValidationService tokenValidationService) {
        this.tokenValidationService = tokenValidationService;
    }
    
    /**
     * Processes the tokens provided by the customer using brand-aware validation.
     * Now includes smart tracking of asked tokens vs. provided tokens for intelligent re-asking logic.
     * Enhanced to support post-validation rules that can trigger additional token requests.
     */
    public void processProvidedTokens(AuthenticationRequest request, AuthenticationContext context, 
                                     CustomerProfile customerProfile) {
        if (request.getProvidedTokens() == null || request.getProvidedTokens().isEmpty()) {
            handleNoTokensProvided(context);
            return;
        }
        
        String brand = context.getBrand();
        String lastAskedToken = context.getLastAskedToken();
        
        logger.debug("Processing {} tokens for brand '{}' and attempt '{}', lastAskedToken: '{}'", 
                    request.getProvidedTokens().size(), brand, context.getAttemptId(), lastAskedToken);
        
        boolean userProvidedAskedToken = checkIfUserProvidedAskedToken(request.getProvidedTokens(), lastAskedToken, context);
        
        processEachProvidedToken(request.getProvidedTokens(), context, customerProfile, lastAskedToken);
        
        handleUnprovidedAskedToken(lastAskedToken, userProvidedAskedToken, context);
    }
    
    /**
     * Handles the case when no tokens are provided by the customer.
     */
    private void handleNoTokensProvided(AuthenticationContext context) {
        if (context.getLastAskedToken() != null) {
            context.decrementTokenAttempts(context.getLastAskedToken());
            context.decrementOverallAttempts();
            
            logger.debug("No tokens provided - decremented attempts for lastAskedToken '{}' for attempt: {}", 
                       context.getLastAskedToken(), context.getAttemptId());
        }
    }
    
    /**
     * Checks if the user provided the token that was specifically asked for.
     */
    private boolean checkIfUserProvidedAskedToken(java.util.List<ProvidedToken> providedTokens, 
                                                 String lastAskedToken, AuthenticationContext context) {
        if (lastAskedToken == null) {
            return false;
        }
        
        for (ProvidedToken providedToken : providedTokens) {
            if (lastAskedToken.equals(providedToken.getTokenName())) {
                logger.debug("User provided the token we asked for: '{}' for attempt: {}", 
                           lastAskedToken, context.getAttemptId());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Processes each individual token provided by the customer.
     * Enhanced to support post-validation rules that can trigger additional token requests.
     */
    private void processEachProvidedToken(java.util.List<ProvidedToken> providedTokens, 
                                        AuthenticationContext context, CustomerProfile customerProfile, 
                                        String lastAskedToken) {
        String brand = context.getBrand();
        
        for (ProvidedToken providedToken : providedTokens) {
            // Use enhanced validation that includes post-validation rules
            TokenValidationResult validationResult = tokenValidationService.validateTokenWithPostValidation(
                    providedToken.getTokenName(),
                    brand,
                    context.getCustomerIdentifier().getValue(),
                    providedToken.getTokenValue(),
                    customerProfile,
                    context
            );
            
            if (validationResult.isValid()) {
                handleSuccessfulValidation(providedToken, context, brand, validationResult);
            } else {
                handleFailedValidation(providedToken, context, lastAskedToken, brand);
            }
        }
    }
    
    /**
     * Handles successful token validation.
     * Enhanced to handle post-validation rules that may require additional tokens.
     */
    private void handleSuccessfulValidation(ProvidedToken providedToken, AuthenticationContext context, 
                                          String brand, TokenValidationResult validationResult) {
        context.addAuthenticatedToken(providedToken.getTokenName());
        logger.debug("Token {} validated successfully for brand '{}' and attempt: {}", 
                   providedToken.getTokenName(), brand, context.getAttemptId());
        
        // Check if post-validation rules require additional tokens
        if (validationResult.requiresAdditionalTokens()) {
            logger.info("Post-validation rules require additional tokens for token '{}': {} (reason: {})", 
                       providedToken.getTokenName(), validationResult.getSuggestedAdditionalTokens(), 
                       validationResult.getReason());
            
            // For now, we'll log this information. In a full implementation, you would:
            // 1. Add fields to AuthenticationContext to store additional token requirements
            // 2. Modify the response service to check for these requirements
            // 3. Prioritize suggested additional tokens in the token selection logic
            logger.debug("Additional tokens suggested: {}, Risk Level: {}", 
                        validationResult.getSuggestedAdditionalTokens(), validationResult.getRiskLevel());
        }
    }
    
    /**
     * Handles failed token validation and applies smart re-asking logic.
     */
    private void handleFailedValidation(ProvidedToken providedToken, AuthenticationContext context, 
                                      String lastAskedToken, String brand) {
        updateTokenAttempts(providedToken, context);
        applySmartReaskingLogic(providedToken, context, lastAskedToken);
        checkAndMarkFailedToken(providedToken, context);
        
        logger.debug("Token {} validation failed for brand '{}' and attempt: {}, remainingAttempts: {}", 
                   providedToken.getTokenName(), brand, context.getAttemptId(), 
                   context.getTokenAttemptsRemaining().get(providedToken.getTokenName()));
    }
    
    /**
     * Updates token and overall attempt counts after a failed validation.
     */
    private void updateTokenAttempts(ProvidedToken providedToken, AuthenticationContext context) {
        context.decrementTokenAttempts(providedToken.getTokenName());
        context.decrementOverallAttempts();
    }
    
    /**
     * Applies smart re-asking logic when a specifically asked token fails validation.
     */
    private void applySmartReaskingLogic(ProvidedToken providedToken, AuthenticationContext context, String lastAskedToken) {
        if (lastAskedToken != null && lastAskedToken.equals(providedToken.getTokenName())) {
            context.markAskedTokenValidationFailure(providedToken.getTokenName());
            logger.debug("User provided asked token '{}' but validation failed - marked for no re-asking for attempt: {}", 
                       providedToken.getTokenName(), context.getAttemptId());
        }
    }
    
    /**
     * Checks if a token has exhausted all attempts and marks it as failed if so.
     */
    private void checkAndMarkFailedToken(ProvidedToken providedToken, AuthenticationContext context) {
        if (!context.hasRemainingAttemptsForToken(providedToken.getTokenName())) {
            context.addFailedToken(providedToken.getTokenName());
            logger.debug("Token {} has no remaining attempts - marked as failed for attempt: {}", 
                       providedToken.getTokenName(), context.getAttemptId());
        }
    }
    
    /**
     * Handles the case when the user didn't provide the specifically asked token.
     */
    private void handleUnprovidedAskedToken(String lastAskedToken, boolean userProvidedAskedToken, 
                                          AuthenticationContext context) {
        if (lastAskedToken != null && !userProvidedAskedToken) {
            logger.debug("User didn't provide the asked token '{}' - can re-ask this token later for attempt: {}", 
                       lastAskedToken, context.getAttemptId());
        }
    }
} 