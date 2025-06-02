package com.bank.ivr.auth.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.rule.EligibilityRule;
import com.bank.ivr.auth.rule.PostValidationRule;
import com.bank.ivr.auth.rule.TokenSelectionRule;

/**
 * Service for managing brand-configured rules.
 * Handles rule selection, filtering, and priority management based on brand configuration.
 */
@Service
public class BrandConfiguredRuleService {
    
    private static final Logger logger = LoggerFactory.getLogger(BrandConfiguredRuleService.class);
    
    private final ApplicationContext applicationContext;
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public BrandConfiguredRuleService(ApplicationContext applicationContext, 
                                    BrandAuthConfigurationService brandConfigService) {
        this.applicationContext = applicationContext;
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Gets applicable token selection rules for a brand, sorted by priority.
     */
    public List<TokenSelectionRule> getTokenSelectionRulesForBrand(String brand) {
        BrandAuthConfiguration config = brandConfigService.getBrandConfiguration(brand);
        if (config == null) {
            logger.warn("No configuration found for brand: {}", brand);
            return Collections.emptyList();
        }
        
        List<String> ruleNames = config.getApplicableTokenSelectionRules();
        Map<String, Integer> rulePriorities = config.getRulePriorities();
        
        List<TokenSelectionRule> rules = new ArrayList<>();
        
        for (String ruleName : ruleNames) {
            try {
                TokenSelectionRule rule = applicationContext.getBean(ruleName, TokenSelectionRule.class);
                rules.add(new PrioritizedTokenSelectionRule(rule, rulePriorities.getOrDefault(ruleName, rule.getPriority())));
            } catch (Exception e) {
                logger.error("Failed to load token selection rule '{}' for brand '{}': {}", ruleName, brand, e.getMessage());
            }
        }
        
        // Sort by priority (highest first)
        Collections.sort(rules, new Comparator<TokenSelectionRule>() {
            @Override
            public int compare(TokenSelectionRule r1, TokenSelectionRule r2) {
                return Integer.compare(r2.getPriority(), r1.getPriority());
            }
        });
        
        logger.debug("Loaded {} token selection rules for brand '{}': {}", rules.size(), brand, ruleNames);
        return rules;
    }
    
    /**
     * Gets applicable eligibility rules for a brand, sorted by priority.
     */
    public List<EligibilityRule> getEligibilityRulesForBrand(String brand) {
        BrandAuthConfiguration config = brandConfigService.getBrandConfiguration(brand);
        if (config == null) {
            logger.warn("No configuration found for brand: {}", brand);
            return Collections.emptyList();
        }
        
        List<String> ruleNames = config.getApplicableEligibilityRules();
        Map<String, Integer> rulePriorities = config.getRulePriorities();
        
        List<EligibilityRule> rules = new ArrayList<>();
        
        for (String ruleName : ruleNames) {
            try {
                EligibilityRule rule = applicationContext.getBean(ruleName, EligibilityRule.class);
                rules.add(new PrioritizedEligibilityRule(rule, rulePriorities.getOrDefault(ruleName, rule.getPriority())));
            } catch (Exception e) {
                logger.error("Failed to load eligibility rule '{}' for brand '{}': {}", ruleName, brand, e.getMessage());
            }
        }
        
        // Sort by priority (highest first)
        Collections.sort(rules, new Comparator<EligibilityRule>() {
            @Override
            public int compare(EligibilityRule r1, EligibilityRule r2) {
                return Integer.compare(r2.getPriority(), r1.getPriority());
            }
        });
        
        logger.debug("Loaded {} eligibility rules for brand '{}': {}", rules.size(), brand, ruleNames);
        return rules;
    }
    
    /**
     * Gets applicable post-validation rules for a brand, sorted by priority.
     */
    public List<PostValidationRule> getPostValidationRulesForBrand(String brand) {
        BrandAuthConfiguration config = brandConfigService.getBrandConfiguration(brand);
        if (config == null) {
            logger.warn("No configuration found for brand: {}", brand);
            return Collections.emptyList();
        }
        
        List<String> ruleNames = config.getApplicablePostValidationRules();
        Map<String, Integer> rulePriorities = config.getRulePriorities();
        
        List<PostValidationRule> rules = new ArrayList<>();
        
        for (String ruleName : ruleNames) {
            try {
                PostValidationRule rule = applicationContext.getBean(ruleName, PostValidationRule.class);
                rules.add(new PrioritizedPostValidationRule(rule, rulePriorities.getOrDefault(ruleName, rule.getPriority())));
            } catch (Exception e) {
                logger.error("Failed to load post-validation rule '{}' for brand '{}': {}", ruleName, brand, e.getMessage());
            }
        }
        
        // Sort by priority (highest first)
        Collections.sort(rules, new Comparator<PostValidationRule>() {
            @Override
            public int compare(PostValidationRule r1, PostValidationRule r2) {
                return Integer.compare(r2.getPriority(), r1.getPriority());
            }
        });
        
        logger.debug("Loaded {} post-validation rules for brand '{}': {}", rules.size(), brand, ruleNames);
        return rules;
    }
    
    // Wrapper classes to override priorities
    
    private static class PrioritizedTokenSelectionRule implements TokenSelectionRule {
        private final TokenSelectionRule delegate;
        private final int overridePriority;
        
        public PrioritizedTokenSelectionRule(TokenSelectionRule delegate, int overridePriority) {
            this.delegate = delegate;
            this.overridePriority = overridePriority;
        }
        
        @Override
        public String determineNextToken(com.bank.ivr.auth.model.domain.AuthenticationContext context, 
                                       com.bank.ivr.auth.model.domain.CustomerProfile customerProfile) {
            return delegate.determineNextToken(context, customerProfile);
        }
        
        @Override
        public String handleTokenFailure(com.bank.ivr.auth.model.domain.AuthenticationContext context, 
                                       com.bank.ivr.auth.model.domain.CustomerProfile customerProfile, 
                                       String failedToken) {
            return delegate.handleTokenFailure(context, customerProfile, failedToken);
        }
        
        @Override
        public boolean isApplicable(com.bank.ivr.auth.model.domain.AuthenticationContext context, 
                                  com.bank.ivr.auth.model.domain.CustomerProfile customerProfile) {
            return delegate.isApplicable(context, customerProfile);
        }
        
        @Override
        public int getPriority() {
            return overridePriority; // Use brand-configured priority
        }
        
        @Override
        public String getBrand() {
            return delegate.getBrand();
        }
        
        @Override
        public String getRuleName() {
            return delegate.getRuleName();
        }
    }
    
    private static class PrioritizedEligibilityRule implements EligibilityRule {
        private final EligibilityRule delegate;
        private final int overridePriority;
        
        public PrioritizedEligibilityRule(EligibilityRule delegate, int overridePriority) {
            this.delegate = delegate;
            this.overridePriority = overridePriority;
        }
        
        @Override
        public boolean isEligible(com.bank.ivr.auth.model.domain.CustomerProfile customerProfile, String brand) {
            return delegate.isEligible(customerProfile, brand);
        }
        
        @Override
        public String getTokenName() {
            return delegate.getTokenName();
        }
        
        @Override
        public String getBrand() {
            return delegate.getBrand();
        }
        
        @Override
        public int getPriority() {
            return overridePriority; // Use brand-configured priority
        }
        
        @Override
        public String getRuleName() {
            return delegate.getRuleName();
        }
    }
    
    private static class PrioritizedPostValidationRule implements PostValidationRule {
        private final PostValidationRule delegate;
        private final int overridePriority;
        
        public PrioritizedPostValidationRule(PostValidationRule delegate, int overridePriority) {
            this.delegate = delegate;
            this.overridePriority = overridePriority;
        }
        
        @Override
        public com.bank.ivr.auth.model.domain.TokenValidationResult evaluatePostValidation(String validatedToken, 
                com.bank.ivr.auth.model.domain.AuthenticationContext context, 
                com.bank.ivr.auth.model.domain.CustomerProfile customerProfile) {
            return delegate.evaluatePostValidation(validatedToken, context, customerProfile);
        }
        
        @Override
        public boolean isApplicable(String validatedToken, 
                                  com.bank.ivr.auth.model.domain.AuthenticationContext context, 
                                  com.bank.ivr.auth.model.domain.CustomerProfile customerProfile) {
            return delegate.isApplicable(validatedToken, context, customerProfile);
        }
        
        @Override
        public List<String> getApplicableTokens() {
            return delegate.getApplicableTokens();
        }
        
        @Override
        public int getPriority() {
            return overridePriority; // Use brand-configured priority
        }
        
        @Override
        public String getBrand() {
            return delegate.getBrand();
        }
        
        @Override
        public String getRuleName() {
            return delegate.getRuleName();
        }
    }
} 