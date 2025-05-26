package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.rule.TokenSelectionRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for determining which authentication token to ask for next.
 * Uses token selection rules with proper priority ordering and brand filtering.
 */
@Service
public class TokenSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenSelectionService.class);
    
    private final List<TokenSelectionRule> tokenSelectionRules;
    
    @Autowired
    public TokenSelectionService(List<TokenSelectionRule> tokenSelectionRules) {
        this.tokenSelectionRules = tokenSelectionRules;
    }
    
    /**
     * Determines the next token to ask using token selection rules.
     * Rules are evaluated in priority order (highest first).
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
        
        logger.debug("Determining next token using {} selection rules for brand: {}", 
                    tokenSelectionRules.size(), brand);
        
        // First, try token selection rules (highest priority first)
        String selectedTokenName = evaluateTokenSelectionRules(context, customerProfile, brand);
        
        if (selectedTokenName != null) {
            // Find the token definition for the selected token
            AuthTokenDefinition selectedToken = findTokenDefinition(selectedTokenName, brandTokenDefinitions);
            if (selectedToken != null && isTokenAvailable(selectedToken, context)) {
                logger.debug("Token selection rule selected token: {} for brand: {}", selectedTokenName, brand);
                return selectedToken;
            } else {
                logger.debug("Token selection rule selected unavailable token: {} for brand: {}", selectedTokenName, brand);
            }
        }
        
        // Fallback to priority-based selection from available tokens
        return fallbackToPriorityBasedSelection(context, brandTokenDefinitions);
    }
    
    /**
     * Handles token failure scenarios using token selection rules.
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
        
        logger.debug("Handling token failure for token: {} using selection rules for brand: {}", failedToken, brand);
        
        // Get applicable rules for this brand, sorted by priority
        List<TokenSelectionRule> applicableRules = getApplicableRules(context, customerProfile, brand);
        
        for (TokenSelectionRule rule : applicableRules) {
            try {
                String escalationToken = rule.handleTokenFailure(context, customerProfile, failedToken);
                if (escalationToken != null) {
                    AuthTokenDefinition escalationTokenDef = findTokenDefinition(escalationToken, brandTokenDefinitions);
                    if (escalationTokenDef != null && isTokenAvailable(escalationTokenDef, context)) {
                        logger.debug("Token selection rule '{}' escalated failed token '{}' to '{}' for brand: {}", 
                                   rule.getRuleName(), failedToken, escalationToken, brand);
                        return escalationTokenDef;
                    }
                }
            } catch (Exception e) {
                logger.error("Error in token selection rule '{}' handling failure for token '{}': {}", 
                           rule.getRuleName(), failedToken, e.getMessage());
            }
        }
        
        logger.debug("No token selection rules provided escalation for failed token: {} in brand: {}", failedToken, brand);
        return null;
    }
    
    /**
     * Evaluates token selection rules in priority order.
     */
    private String evaluateTokenSelectionRules(AuthenticationContext context, CustomerProfile customerProfile, String brand) {
        // Get applicable rules for this brand, sorted by priority
        List<TokenSelectionRule> applicableRules = getApplicableRules(context, customerProfile, brand);
        
        for (TokenSelectionRule rule : applicableRules) {
            try {
                String selectedToken = rule.determineNextToken(context, customerProfile);
                if (selectedToken != null) {
                    logger.debug("Token selection rule '{}' selected token: {} for brand: {}", 
                               rule.getRuleName(), selectedToken, brand);
                    return selectedToken;
                }
            } catch (Exception e) {
                logger.error("Error evaluating token selection rule '{}': {}", rule.getRuleName(), e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Gets applicable rules for the current context, filtered by brand and sorted by priority.
     */
    private List<TokenSelectionRule> getApplicableRules(AuthenticationContext context, 
                                                       CustomerProfile customerProfile, 
                                                       String brand) {
        return tokenSelectionRules.stream()
                .filter(rule -> isBrandApplicable(rule, brand))
                .filter(rule -> rule.isApplicable(context, customerProfile))
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority())) // Highest priority first
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a rule applies to the given brand.
     */
    private boolean isBrandApplicable(TokenSelectionRule rule, String brand) {
        String ruleBrand = rule.getBrand();
        return "DEFAULT".equals(ruleBrand) || brand.equals(ruleBrand);
    }
    
    /**
     * Finds a token definition by name.
     */
    private AuthTokenDefinition findTokenDefinition(String tokenName, List<AuthTokenDefinition> brandTokenDefinitions) {
        return brandTokenDefinitions.stream()
                .filter(token -> token.getName().equals(tokenName))
                .findFirst()
                .orElse(null);
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
     * Fallback to priority-based token selection when no rules apply.
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