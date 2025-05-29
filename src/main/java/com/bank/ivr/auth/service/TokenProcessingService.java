package com.bank.ivr.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.DnisConfiguration;
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
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public TokenProcessingService(TokenValidationService tokenValidationService,
                                 BrandAuthConfigurationService brandConfigService) {
        this.tokenValidationService = tokenValidationService;
        this.brandConfigService = brandConfigService;
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
        
        // Get all available token definitions for secondary token detection
        List<AuthTokenDefinition> availableTokens = brandConfigService.getTokenDefinitionsForBrand(brand);
        
        // Detect any secondary tokens in the provided input
        List<ProvidedToken> allDetectedTokens = detectSecondaryTokens(request.getProvidedTokens(), availableTokens);
        
        boolean userProvidedAskedToken = checkIfUserProvidedAskedToken(allDetectedTokens, lastAskedToken, context);
        
        processEachProvidedToken(allDetectedTokens, context, customerProfile, lastAskedToken);
        
        handleUnprovidedAskedToken(lastAskedToken, userProvidedAskedToken, context);
    }
    
    /**
     * Detects secondary tokens based on inputFormatRegex patterns.
     * This allows the system to identify additional authentication factors from user input.
     */
    private List<ProvidedToken> detectSecondaryTokens(List<ProvidedToken> providedTokens, 
                                                     List<AuthTokenDefinition> availableTokens) {
        List<ProvidedToken> allTokens = new ArrayList<>(providedTokens);
        
        // For each provided token value, check if it matches patterns for other token types
        for (ProvidedToken providedToken : providedTokens) {
            String tokenValue = providedToken.getTokenValue();
            
            // Check against all available token patterns
            for (AuthTokenDefinition tokenDef : availableTokens) {
                // Skip if this is already the identified token type
                if (tokenDef.getName().equals(providedToken.getTokenName())) {
                    continue;
                }
                
                // Skip if we already have this token type
                boolean alreadyHaveThisType = allTokens.stream()
                    .anyMatch(t -> t.getTokenName().equals(tokenDef.getName()));
                if (alreadyHaveThisType) {
                    continue;
                }
                
                // Check if the value matches this token's pattern
                if (matchesTokenPattern(tokenValue, tokenDef)) {
                    logger.debug("Detected secondary token '{}' from input value matching pattern: {}", 
                               tokenDef.getName(), tokenDef.getInputFormatRegex());
                    
                    // Add as a secondary detected token
                    ProvidedToken secondaryToken = new ProvidedToken(tokenDef.getName(), tokenValue);
                    allTokens.add(secondaryToken);
                }
            }
        }
        
        return allTokens;
    }
    
    /**
     * Checks if a token value matches the input format regex pattern for a token definition.
     */
    private boolean matchesTokenPattern(String tokenValue, AuthTokenDefinition tokenDef) {
        if (tokenValue == null || tokenDef.getInputFormatRegex() == null) {
            return false;
        }
        
        try {
            Pattern pattern = Pattern.compile(tokenDef.getInputFormatRegex());
            return pattern.matcher(tokenValue.trim()).matches();
        } catch (Exception e) {
            logger.warn("Invalid regex pattern for token {}: {}", tokenDef.getName(), tokenDef.getInputFormatRegex());
            return false;
        }
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
     * Modified to not fail authentication immediately but continue with other available tokens.
     */
    private void handleFailedValidation(ProvidedToken providedToken, AuthenticationContext context, 
                                      String lastAskedToken, String brand) {
        updateTokenAttempts(providedToken, context);
        applySmartReaskingLogic(providedToken, context, lastAskedToken);
        checkAndMarkFailedToken(providedToken, context);
        
        logger.debug("Token {} validation failed for brand '{}' and attempt: {}, remainingAttempts: {} - continuing with other tokens", 
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
    
    /**
     * Processes the tokens provided by the customer using brand-aware validation with DNIS configuration.
     * Enhanced to detect secondary tokens and apply DNIS-specific rules.
     */
    public AuthenticationContext processTokensWithDnis(java.util.List<ProvidedToken> providedTokens, 
                                                      AuthenticationContext context, 
                                                      CustomerProfile customerProfile, 
                                                      String brand, 
                                                      DnisConfiguration dnisConfig) {
        
        if (providedTokens == null || providedTokens.isEmpty()) {
            handleNoTokensProvidedWithDnis(context, dnisConfig);
            return context;
        }
        
        String lastAskedToken = context.getLastAskedToken();
        
        logger.debug("Processing {} tokens with DNIS {} for brand '{}' and attempt '{}', lastAskedToken: '{}'", 
                    providedTokens.size(), dnisConfig.getDnis(), brand, context.getAttemptId(), lastAskedToken);
        
        // Get all available token definitions for secondary token detection
        List<AuthTokenDefinition> availableTokens = brandConfigService.getTokenDefinitionsForBrand(brand);
        
        // Detect any secondary tokens in the provided input
        List<ProvidedToken> allDetectedTokens = detectSecondaryTokens(providedTokens, availableTokens);
        
        boolean userProvidedAskedToken = checkIfUserProvidedAskedToken(allDetectedTokens, lastAskedToken, context);
        
        processEachProvidedTokenWithDnis(allDetectedTokens, context, customerProfile, lastAskedToken, brand, dnisConfig);
        
        handleUnprovidedAskedToken(lastAskedToken, userProvidedAskedToken, context);
        
        return context;
    }
    
    /**
     * Handles the case when no tokens are provided by the customer with DNIS consideration.
     */
    private void handleNoTokensProvidedWithDnis(AuthenticationContext context, DnisConfiguration dnisConfig) {
        if (context.getLastAskedToken() != null) {
            // Apply DNIS-specific retry logic
            if (dnisConfig.isAllowRetryOnFailure()) {
                context.decrementTokenAttempts(context.getLastAskedToken());
                context.decrementOverallAttempts();
                
                logger.debug("No tokens provided - decremented attempts for lastAskedToken '{}' for attempt: {} (DNIS allows retry)", 
                           context.getLastAskedToken(), context.getAttemptId());
            } else {
                // DNIS doesn't allow retry - mark token as failed immediately
                context.addFailedToken(context.getLastAskedToken());
                context.decrementOverallAttempts();
                
                logger.debug("No tokens provided - marked lastAskedToken '{}' as failed for attempt: {} (DNIS strict mode)", 
                           context.getLastAskedToken(), context.getAttemptId());
            }
        }
    }
    
    /**
     * Processes each individual token provided by the customer with DNIS configuration.
     */
    private void processEachProvidedTokenWithDnis(java.util.List<ProvidedToken> providedTokens, 
                                                 AuthenticationContext context, 
                                                 CustomerProfile customerProfile, 
                                                 String lastAskedToken, 
                                                 String brand, 
                                                 DnisConfiguration dnisConfig) {
        for (ProvidedToken providedToken : providedTokens) {
            // Check if token is allowed by DNIS configuration
            if (!isTokenAllowedByDnis(providedToken.getTokenName(), dnisConfig)) {
                logger.warn("Token '{}' is not allowed by DNIS configuration '{}' - rejecting", 
                           providedToken.getTokenName(), dnisConfig.getDnis());
                handleDnisBlockedToken(providedToken, context, dnisConfig);
                continue;
            }
            
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
                handleSuccessfulValidationWithDnis(providedToken, context, brand, validationResult, dnisConfig);
            } else {
                handleFailedValidationWithDnis(providedToken, context, lastAskedToken, brand, dnisConfig);
            }
        }
    }
    
    /**
     * Checks if a token is allowed by the DNIS configuration.
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
                // Allow unknown tokens by default (backward compatibility)
                return true;
        }
    }
    
    /**
     * Handles tokens that are blocked by DNIS configuration.
     */
    private void handleDnisBlockedToken(ProvidedToken providedToken, AuthenticationContext context, DnisConfiguration dnisConfig) {
        if (dnisConfig.isEnableStrictValidation()) {
            // In strict mode, DNIS-blocked tokens count as failed attempts
            context.decrementOverallAttempts();
            context.addFailedToken(providedToken.getTokenName());
            logger.debug("Token {} blocked by DNIS strict mode - marked as failed for attempt: {}", 
                       providedToken.getTokenName(), context.getAttemptId());
        } else {
            // In non-strict mode, just ignore the token
            logger.debug("Token {} blocked by DNIS but not counting as failed attempt (non-strict mode) for attempt: {}", 
                       providedToken.getTokenName(), context.getAttemptId());
        }
    }
    
    /**
     * Handles successful token validation with DNIS consideration.
     */
    private void handleSuccessfulValidationWithDnis(ProvidedToken providedToken, AuthenticationContext context, 
                                                   String brand, TokenValidationResult validationResult, DnisConfiguration dnisConfig) {
        context.addAuthenticatedToken(providedToken.getTokenName());
        logger.debug("Token {} validated successfully for brand '{}', DNIS '{}' and attempt: {}", 
                   providedToken.getTokenName(), brand, dnisConfig.getDnis(), context.getAttemptId());
        
        // Check if post-validation rules require additional tokens (considering DNIS multi-factor requirements)
        if (validationResult.requiresAdditionalTokens() || dnisConfig.isRequireMultiFactorAuth()) {
            logger.info("Additional tokens required for token '{}': {} (reason: {}, DNIS MFA: {})", 
                       providedToken.getTokenName(), validationResult.getSuggestedAdditionalTokens(), 
                       validationResult.getReason(), dnisConfig.isRequireMultiFactorAuth());
        }
    }
    
    /**
     * Handles failed token validation with DNIS consideration.
     * Modified to not fail authentication immediately but continue with other available tokens.
     */
    private void handleFailedValidationWithDnis(ProvidedToken providedToken, AuthenticationContext context, 
                                               String lastAskedToken, String brand, DnisConfiguration dnisConfig) {
        updateTokenAttemptsWithDnis(providedToken, context, dnisConfig);
        applySmartReaskingLogicWithDnis(providedToken, context, lastAskedToken, dnisConfig);
        checkAndMarkFailedTokenWithDnis(providedToken, context, dnisConfig);
        
        logger.debug("Token {} validation failed for brand '{}', DNIS '{}' and attempt: {}, remainingAttempts: {} - continuing with other tokens", 
                   providedToken.getTokenName(), brand, dnisConfig.getDnis(), context.getAttemptId(), 
                   context.getTokenAttemptsRemaining().get(providedToken.getTokenName()));
    }
    
    /**
     * Updates token and overall attempt counts after a failed validation with DNIS consideration.
     */
    private void updateTokenAttemptsWithDnis(ProvidedToken providedToken, AuthenticationContext context, DnisConfiguration dnisConfig) {
        if (dnisConfig.isAllowRetryOnFailure()) {
            context.decrementTokenAttempts(providedToken.getTokenName());
            context.decrementOverallAttempts();
        } else {
            // DNIS doesn't allow retry - mark as failed immediately
            context.getTokenAttemptsRemaining().put(providedToken.getTokenName(), 0);
            context.decrementOverallAttempts();
            logger.debug("DNIS strict mode - token {} marked with 0 attempts remaining", providedToken.getTokenName());
        }
    }
    
    /**
     * Applies smart re-asking logic when a specifically asked token fails validation with DNIS consideration.
     */
    private void applySmartReaskingLogicWithDnis(ProvidedToken providedToken, AuthenticationContext context, 
                                                String lastAskedToken, DnisConfiguration dnisConfig) {
        if (lastAskedToken != null && lastAskedToken.equals(providedToken.getTokenName())) {
            context.markAskedTokenValidationFailure(providedToken.getTokenName());
            logger.debug("User provided asked token '{}' but validation failed - marked for no re-asking for attempt: {} (DNIS: {})", 
                       providedToken.getTokenName(), context.getAttemptId(), dnisConfig.getDnis());
        }
    }
    
    /**
     * Checks if a token has exhausted all attempts and marks it as failed if so with DNIS consideration.
     */
    private void checkAndMarkFailedTokenWithDnis(ProvidedToken providedToken, AuthenticationContext context, DnisConfiguration dnisConfig) {
        if (!context.hasRemainingAttemptsForToken(providedToken.getTokenName())) {
            context.addFailedToken(providedToken.getTokenName());
            logger.debug("Token {} has no remaining attempts - marked as failed for attempt: {} (DNIS: {})", 
                       providedToken.getTokenName(), context.getAttemptId(), dnisConfig.getDnis());
        }
    }
} 