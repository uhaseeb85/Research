package com.bank.ivr.auth.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;
import com.bank.ivr.auth.rule.PostValidationRule;

/**
 * Service responsible for evaluating post-validation rules to determine
 * if additional authentication tokens should be requested after successful
 * token validation.
 */
@Service
public class PostValidationRuleService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostValidationRuleService.class);
    
    private final List<PostValidationRule> postValidationRules;
    
    @Autowired
    public PostValidationRuleService(List<PostValidationRule> postValidationRules) {
        this.postValidationRules = postValidationRules;
        logger.info("Initialized PostValidationRuleService with {} rules", postValidationRules.size());
    }
    
    /**
     * Evaluates all applicable post-validation rules for a successfully validated token.
     * Rules are evaluated in priority order (highest first).
     * 
     * @param validatedToken the token that was successfully validated
     * @param context the authentication context
     * @param customerProfile the customer profile
     * @return TokenValidationResult indicating if additional tokens are needed
     */
    public TokenValidationResult evaluatePostValidation(String validatedToken,
                                                       AuthenticationContext context,
                                                       CustomerProfile customerProfile) {
        String brand = context.getBrand();
        
        logger.debug("Evaluating post-validation rules for token '{}', brand '{}', attempt '{}'", 
                    validatedToken, brand, context.getAttemptId());
        
        // Get applicable rules for this brand and token, sorted by priority
        List<PostValidationRule> applicableRules = getApplicableRules(validatedToken, context, customerProfile, brand);
        
        if (applicableRules.isEmpty()) {
            logger.debug("No applicable post-validation rules found for token '{}', brand '{}'", validatedToken, brand);
            return TokenValidationResult.success();
        }
        
        // Evaluate rules in priority order - first rule that requires additional tokens wins
        for (PostValidationRule rule : applicableRules) {
            try {
                TokenValidationResult result = rule.evaluatePostValidation(validatedToken, context, customerProfile);
                
                if (result.requiresAdditionalTokens()) {
                    logger.info("Post-validation rule '{}' requires additional tokens for token '{}', brand '{}': {}",
                               rule.getRuleName(), validatedToken, brand, result.getReason());
                    return result;
                }
                
                logger.debug("Post-validation rule '{}' does not require additional tokens for token '{}', brand '{}'",
                           rule.getRuleName(), validatedToken, brand);
                
            } catch (Exception e) {
                logger.error("Error evaluating post-validation rule '{}' for token '{}', brand '{}': {}",
                           rule.getRuleName(), validatedToken, brand, e.getMessage(), e);
            }
        }
        
        logger.debug("No post-validation rules require additional tokens for token '{}', brand '{}'", validatedToken, brand);
        return TokenValidationResult.success();
    }
    
    /**
     * Gets applicable rules for the current context, filtered by brand and token, sorted by priority.
     */
    private List<PostValidationRule> getApplicableRules(String validatedToken,
                                                       AuthenticationContext context,
                                                       CustomerProfile customerProfile,
                                                       String brand) {
        return postValidationRules.stream()
                .filter(rule -> isBrandApplicable(rule, brand))
                .filter(rule -> isTokenApplicable(rule, validatedToken))
                .filter(rule -> rule.isApplicable(validatedToken, context, customerProfile))
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority())) // Highest priority first
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a rule applies to the given brand.
     */
    private boolean isBrandApplicable(PostValidationRule rule, String brand) {
        String ruleBrand = rule.getBrand();
        return "DEFAULT".equals(ruleBrand) || brand.equals(ruleBrand);
    }
    
    /**
     * Checks if a rule applies to the given token.
     */
    private boolean isTokenApplicable(PostValidationRule rule, String tokenName) {
        List<String> applicableTokens = rule.getApplicableTokens();
        // If empty list, rule applies to all tokens
        return applicableTokens.isEmpty() || applicableTokens.contains(tokenName);
    }
    
    /**
     * Gets all registered post-validation rules for debugging.
     */
    public List<PostValidationRule> getAllRules() {
        return postValidationRules;
    }
    
    /**
     * Gets rules applicable to a specific brand.
     */
    public List<PostValidationRule> getRulesForBrand(String brand) {
        return postValidationRules.stream()
                .filter(rule -> isBrandApplicable(rule, brand))
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .collect(Collectors.toList());
    }
} 