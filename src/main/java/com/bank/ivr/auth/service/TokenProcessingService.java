package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.ProvidedToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for processing and validating customer-provided tokens.
 * Now supports brand-aware token validation.
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
     */
    public void processProvidedTokens(AuthenticationRequest request, AuthenticationContext context, 
                                     CustomerProfile customerProfile) {
        if (request.getProvidedTokens() == null || request.getProvidedTokens().isEmpty()) {
            // No tokens provided - decrement attempts for last asked token
            if (context.getLastAskedToken() != null) {
                context.decrementTokenAttempts(context.getLastAskedToken());
                context.decrementOverallAttempts();
                
                logger.debug("No tokens provided - decremented attempts for lastAskedToken '{}' for attempt: {}", 
                           context.getLastAskedToken(), context.getAttemptId());
            }
            return;
        }
        
        String brand = context.getBrand();
        String lastAskedToken = context.getLastAskedToken();
        boolean userProvidedAskedToken = false;
        
        logger.debug("Processing {} tokens for brand '{}' and attempt '{}', lastAskedToken: '{}'", 
                    request.getProvidedTokens().size(), brand, context.getAttemptId(), lastAskedToken);
        
        // First, check if user provided the token we specifically asked for
        for (ProvidedToken providedToken : request.getProvidedTokens()) {
            if (lastAskedToken != null && lastAskedToken.equals(providedToken.getTokenName())) {
                userProvidedAskedToken = true;
                logger.debug("User provided the token we asked for: '{}' for attempt: {}", 
                           lastAskedToken, context.getAttemptId());
                break;
            }
        }
        
        // Process each provided token
        for (ProvidedToken providedToken : request.getProvidedTokens()) {
            boolean isValid = tokenValidationService.validateToken(
                    providedToken.getTokenName(),
                    brand,
                    context.getCustomerIdentifier().getValue(),
                    providedToken.getTokenValue(),
                    customerProfile
            );
            
            if (isValid) {
                context.addAuthenticatedToken(providedToken.getTokenName());
                logger.debug("Token {} validated successfully for brand '{}' and attempt: {}", 
                           providedToken.getTokenName(), brand, context.getAttemptId());
            } else {
                context.decrementTokenAttempts(providedToken.getTokenName());
                context.decrementOverallAttempts();
                
                // SMART RE-ASKING LOGIC: If user provided the token we specifically asked for
                // but it failed validation, mark it to prevent re-asking the same token
                if (lastAskedToken != null && lastAskedToken.equals(providedToken.getTokenName())) {
                    context.markAskedTokenValidationFailure(providedToken.getTokenName());
                    logger.debug("User provided asked token '{}' but validation failed - marked for no re-asking for attempt: {}", 
                               providedToken.getTokenName(), context.getAttemptId());
                }
                
                // Check if this token has failed all attempts
                if (!context.hasRemainingAttemptsForToken(providedToken.getTokenName())) {
                    context.addFailedToken(providedToken.getTokenName());
                    logger.debug("Token {} has no remaining attempts - marked as failed for attempt: {}", 
                               providedToken.getTokenName(), context.getAttemptId());
                }
                
                logger.debug("Token {} validation failed for brand '{}' and attempt: {}, remainingAttempts: {}", 
                           providedToken.getTokenName(), brand, context.getAttemptId(), 
                           context.getTokenAttemptsRemaining().get(providedToken.getTokenName()));
            }
        }
        
        // If we asked for a token but user didn't provide it at all, they might provide
        // a different token instead - this is okay and we can re-ask the original token later
        if (lastAskedToken != null && !userProvidedAskedToken) {
            logger.debug("User didn't provide the asked token '{}' - can re-ask this token later for attempt: {}", 
                       lastAskedToken, context.getAttemptId());
        }
    }
} 