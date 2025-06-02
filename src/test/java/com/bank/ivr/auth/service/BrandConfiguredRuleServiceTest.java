package com.bank.ivr.auth.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.rule.TokenSelectionRule;

/**
 * Unit tests for BrandConfiguredRuleService.
 * Tests the new brand-configured rules approach.
 */
@ExtendWith(MockitoExtension.class)
public class BrandConfiguredRuleServiceTest {
    
    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private BrandAuthConfigurationService brandConfigService;
    
    @Mock
    private BrandAuthConfiguration digitalBankConfig;
    
    @Mock
    private TokenSelectionRule trustBasedRule;
    
    @Mock
    private TokenSelectionRule highValueRule;
    
    private BrandConfiguredRuleService brandConfiguredRuleService;
    
    @BeforeEach
    void setUp() {
        brandConfiguredRuleService = new BrandConfiguredRuleService(applicationContext, brandConfigService);
    }
    
    @Test
    void testGetTokenSelectionRulesForBrand_Success() {
        // Arrange
        String brand = "DIGITAL_BANK";
        List<String> ruleNames = Arrays.asList("TRUST_BASED_SECURITY_RULE", "HIGH_VALUE_CUSTOMER_RULE");
        Map<String, Integer> rulePriorities = new HashMap<>();
        rulePriorities.put("TRUST_BASED_SECURITY_RULE", 300);
        rulePriorities.put("HIGH_VALUE_CUSTOMER_RULE", 250);
        
        when(brandConfigService.getBrandConfiguration(brand)).thenReturn(digitalBankConfig);
        when(digitalBankConfig.getApplicableTokenSelectionRules()).thenReturn(ruleNames);
        when(digitalBankConfig.getRulePriorities()).thenReturn(rulePriorities);
        
        when(trustBasedRule.getPriority()).thenReturn(250); // Default priority
        when(highValueRule.getPriority()).thenReturn(200);  // Default priority
        
        when(applicationContext.getBean("TRUST_BASED_SECURITY_RULE", TokenSelectionRule.class))
            .thenReturn(trustBasedRule);
        when(applicationContext.getBean("HIGH_VALUE_CUSTOMER_RULE", TokenSelectionRule.class))
            .thenReturn(highValueRule);
        
        // Act
        List<TokenSelectionRule> result = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Rules should be sorted by brand-configured priority (highest first)
        // Trust-based rule should be first (priority 300)
        assertEquals(300, result.get(0).getPriority());
        // High-value rule should be second (priority 250)
        assertEquals(250, result.get(1).getPriority());
    }
    
    @Test
    void testGetTokenSelectionRulesForBrand_EmptyRulesList() {
        // Arrange
        String brand = "MINIMAL_BANK";
        when(brandConfigService.getBrandConfiguration(brand)).thenReturn(digitalBankConfig);
        when(digitalBankConfig.getApplicableTokenSelectionRules()).thenReturn(Arrays.asList());
        when(digitalBankConfig.getRulePriorities()).thenReturn(new HashMap<>());
        
        // Act
        List<TokenSelectionRule> result = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetTokenSelectionRulesForBrand_BrandConfigurationNotFound() {
        // Arrange
        String brand = "UNKNOWN_BANK";
        when(brandConfigService.getBrandConfiguration(brand)).thenReturn(null);
        
        // Act
        List<TokenSelectionRule> result = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testRulePriorityOverride() {
        // Arrange
        String brand = "PREMIUM_BANK";
        List<String> ruleNames = Arrays.asList("HIGH_VALUE_CUSTOMER_RULE");
        Map<String, Integer> rulePriorities = new HashMap<>();
        rulePriorities.put("HIGH_VALUE_CUSTOMER_RULE", 400); // Brand overrides to higher priority
        
        when(brandConfigService.getBrandConfiguration(brand)).thenReturn(digitalBankConfig);
        when(digitalBankConfig.getApplicableTokenSelectionRules()).thenReturn(ruleNames);
        when(digitalBankConfig.getRulePriorities()).thenReturn(rulePriorities);
        
        when(highValueRule.getPriority()).thenReturn(200);  // Default priority
        when(applicationContext.getBean("HIGH_VALUE_CUSTOMER_RULE", TokenSelectionRule.class))
            .thenReturn(highValueRule);
        
        // Act
        List<TokenSelectionRule> result = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        // Rule should have brand-configured priority, not default
        assertEquals(400, result.get(0).getPriority());
    }
    
    @Test
    void testRuleLoadingFailure() {
        // Arrange
        String brand = "TEST_BANK";
        List<String> ruleNames = Arrays.asList("NONEXISTENT_RULE", "HIGH_VALUE_CUSTOMER_RULE");
        Map<String, Integer> rulePriorities = new HashMap<>();
        
        when(brandConfigService.getBrandConfiguration(brand)).thenReturn(digitalBankConfig);
        when(digitalBankConfig.getApplicableTokenSelectionRules()).thenReturn(ruleNames);
        when(digitalBankConfig.getRulePriorities()).thenReturn(rulePriorities);
        
        // First rule fails to load
        when(applicationContext.getBean("NONEXISTENT_RULE", TokenSelectionRule.class))
            .thenThrow(new RuntimeException("Bean not found"));
        
        // Second rule loads successfully
        when(highValueRule.getPriority()).thenReturn(200);
        when(applicationContext.getBean("HIGH_VALUE_CUSTOMER_RULE", TokenSelectionRule.class))
            .thenReturn(highValueRule);
        
        // Act
        List<TokenSelectionRule> result = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size()); // Only the successful rule should be loaded
        assertEquals(200, result.get(0).getPriority());
    }
} 