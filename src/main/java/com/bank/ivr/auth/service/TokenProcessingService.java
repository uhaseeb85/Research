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
     */
    public void processProvidedTokens(AuthenticationRequest request, AuthenticationContext context, 
                                     CustomerProfile customerProfile) {
        if (request.getProvidedTokens() == null || request.getProvidedTokens().isEmpty()) {
            // No tokens provided - decrement attempts for last asked token
            if (context.getLastAskedToken() != null) {
                context.decrementTokenAttempts(context.getLastAskedToken());
                context.decrementOverallAttempts();
            }
            return;
        }
        
        String brand = context.getBrand();
        logger.debug("Processing {} tokens for brand '{}' and attempt '{}'", 
                    request.getProvidedTokens().size(), brand, context.getAttemptId());
        
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
                
                // Check if this token has failed all attempts
                if (!context.hasRemainingAttemptsForToken(providedToken.getTokenName())) {
                    context.addFailedToken(providedToken.getTokenName());
                }
                
                logger.debug("Token {} validation failed for brand '{}' and attempt: {}", 
                           providedToken.getTokenName(), brand, context.getAttemptId());
            }
        }
    }
} 