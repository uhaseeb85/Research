package com.bank.ivr.auth.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.TokenSelectionRule;

/**
 * Service responsible for determining which authentication token to ask for next.
 * Uses brand-configured token selection rules with proper priority ordering.
 */
@Service
public class TokenSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenSelectionService.class);
    
    private final BrandConfiguredRuleService brandConfiguredRuleService;
    
    @Autowired
    public TokenSelectionService(BrandConfiguredRuleService brandConfiguredRuleService) {
        this.brandConfiguredRuleService = brandConfiguredRuleService;
    }
    
    /**
     * Determines the next token to ask using brand-configured token selection rules.
     * Rules are evaluated in brand-configured priority order (highest first).
     * 
     * @param context the authentication context
     * @param customerProfile the customer profile
     * @param brandTokenDefinitions available token definitions for the brand
     * @return the token definition to ask next, or null if no suitable token found
     */
    public AuthTokenDefinition determineNextToken(AuthenticationContext context, 
                                                 CustomerProfile customerProfile,
                                                 List<AuthTokenDefinition> brandTokenDefinitions) {
        String brand = context.getBrand();
        
        logger.debug("Determining next token using brand-configured rules for brand: {}", brand);
        
        // Get brand-configured token selection rules
        List<TokenSelectionRule> applicableRules = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
        
        // First, try brand-configured token selection rules (highest priority first)
        String selectedTokenName = evaluateTokenSelectionRules(applicableRules, context, customerProfile, brand);
        
        if (selectedTokenName != null) {
            // Find the token definition for the selected token
            AuthTokenDefinition selectedToken = findTokenDefinition(selectedTokenName, brandTokenDefinitions);
            if (selectedToken != null && isTokenAvailable(selectedToken, context)) {
                logger.debug("Brand-configured rule selected token: {} for brand: {}", selectedTokenName, brand);
                return selectedToken;
            } else {
                logger.debug("Brand-configured rule selected unavailable token: {} for brand: {}", selectedTokenName, brand);
            }
        }
        
        // Fallback to priority-based selection from available tokens
        return fallbackToPriorityBasedSelection(context, brandTokenDefinitions);
    }
    
    /**
     * Handles token failure scenarios using brand-configured token selection rules.
     * 
     * @param context the authentication context
     * @param customerProfile the customer profile
     * @param failedToken the token that failed validation
     * @param brandTokenDefinitions available token definitions
     * @return the escalation token definition, or null if no escalation available
     */
    public AuthTokenDefinition handleTokenFailure(AuthenticationContext context,
                                                 CustomerProfile customerProfile,
                                                 String failedToken,
                                                 List<AuthTokenDefinition> brandTokenDefinitions) {
        String brand = context.getBrand();
        
        logger.debug("Handling token failure for token: {} using brand-configured rules for brand: {}", failedToken, brand);
        
        // Get brand-configured token selection rules
        List<TokenSelectionRule> applicableRules = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
        
        for (TokenSelectionRule rule : applicableRules) {
            if (rule.isApplicable(context, customerProfile)) {
                try {
                    String escalationToken = rule.handleTokenFailure(context, customerProfile, failedToken);
                    if (escalationToken != null) {
                        AuthTokenDefinition escalationTokenDef = findTokenDefinition(escalationToken, brandTokenDefinitions);
                        if (escalationTokenDef != null && isTokenAvailable(escalationTokenDef, context)) {
                            logger.debug("Brand-configured rule '{}' escalated failed token '{}' to '{}' for brand: {}", 
                                       rule.getRuleName(), failedToken, escalationToken, brand);
                            return escalationTokenDef;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error in brand-configured rule '{}' handling failure for token '{}': {}", 
                               rule.getRuleName(), failedToken, e.getMessage());
                }
            }
        }
        
        logger.debug("No brand-configured rules provided escalation for failed token: {} in brand: {}", failedToken, brand);
        return null;
    }
    
    /**
     * Evaluates brand-configured token selection rules in priority order.
     */
    private String evaluateTokenSelectionRules(List<TokenSelectionRule> applicableRules, 
                                             AuthenticationContext context, 
                                             CustomerProfile customerProfile, 
                                             String brand) {
        
        for (TokenSelectionRule rule : applicableRules) {
            if (rule.isApplicable(context, customerProfile)) {
                try {
                    String selectedToken = rule.determineNextToken(context, customerProfile);
                    if (selectedToken != null) {
                        logger.debug("Brand-configured rule '{}' selected token: {} for brand: {}", 
                                   rule.getRuleName(), selectedToken, brand);
                        return selectedToken;
                    }
                } catch (Exception e) {
                    logger.error("Error evaluating brand-configured token selection rule '{}': {}", rule.getRuleName(), e.getMessage());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Finds a token definition by name.
     */
    private AuthTokenDefinition findTokenDefinition(String tokenName, List<AuthTokenDefinition> brandTokenDefinitions) {
        for (AuthTokenDefinition token : brandTokenDefinitions) {
            if (token.getName().equals(tokenName)) {
                return token;
            }
        }
        return null;
    }
    
    /**
     * Checks if a token is available for use (eligible, not authenticated, not failed, has attempts).
     */
    private boolean isTokenAvailable(AuthTokenDefinition token, AuthenticationContext context) {
        String tokenName = token.getName();
        
        return context.getEligibleTokens().contains(tokenName) &&
               !context.isTokenAuthenticated(tokenName) &&
               !context.isTokenFailed(tokenName) &&
               context.hasRemainingAttemptsForToken(tokenName) &&
               context.canReAskToken(tokenName);
    }
    
    /**
     * Fallback to priority-based token selection when no brand-configured rules apply.
     */
    private AuthTokenDefinition fallbackToPriorityBasedSelection(AuthenticationContext context, 
                                                               List<AuthTokenDefinition> brandTokenDefinitions) {
        logger.debug("Falling back to priority-based token selection for attempt: {}", context.getAttemptId());
        
        AuthTokenDefinition bestToken = null;
        int highestPriority = -1;
        
        for (AuthTokenDefinition token : brandTokenDefinitions) {
            if (isTokenAvailable(token, context) && token.getPriority() > highestPriority) {
                bestToken = token;
                highestPriority = token.getPriority();
            }
        }
        
        if (bestToken != null) {
            logger.debug("Priority-based selection chose token: {} with priority: {}", 
                       bestToken.getName(), bestToken.getPriority());
        }
        
        return bestToken;
    }
} 