package com.bank.ivr.auth.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.bank.ivr.auth.config.BrandAuthConfiguration;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.rule.TokenSelectionRule;
import com.bank.ivr.auth.rule.impl.HighValueCustomerRule;
import com.bank.ivr.auth.rule.impl.TrustBasedSecurityRule;

/**
 * Comprehensive unit tests for BrandConfiguredRuleService.
 * Tests the new brand-configured rules approach with context-aware functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Brand Configured Rule Service Tests")
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
    
    @Nested
    @DisplayName("Basic Brand Configuration Tests")
    class BasicBrandConfigurationTests {
        
        @Test
        @DisplayName("Should get token selection rules for brand successfully")
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
        @DisplayName("Should return empty list when no rules configured")
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
        @DisplayName("Should return empty list when brand configuration not found")
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
        @DisplayName("Should override rule priority with brand configuration")
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
        @DisplayName("Should handle rule loading failure gracefully")
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
    
    @Nested
    @DisplayName("Context-Aware Rules Integration Tests")
    class ContextAwareRulesIntegrationTests {
        
        private AuthenticationContext createTestContext(TrustLevelInfo trustLevelInfo) {
            Map<String, Integer> tokenAttempts = new HashMap<>();
            tokenAttempts.put("SSN_FULL", 3);
            tokenAttempts.put("SSN", 3);
            tokenAttempts.put("SSN_LAST_4", 3);
            tokenAttempts.put("MOBILE_PIN", 3);
            tokenAttempts.put("DEBIT_CARD_PIN", 3);
            
            return AuthenticationContext.builder()
                    .attemptId("test-attempt-123")
                    .sessionId("test-session-456")
                    .customerIdentifier(new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "555-1234"))
                    .brand("TEST_BANK")
                    .startTime(LocalDateTime.now())
                    .trustLevelInfo(trustLevelInfo)
                    .tokenAttemptsRemaining(tokenAttempts)
                    .overallAttemptsRemaining(5)
                    .eligibleTokens(Arrays.asList("SSN_FULL", "SSN", "SSN_LAST_4", "MOBILE_PIN", "DEBIT_CARD_PIN"))
                    .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .build();
        }
        
        private CustomerProfile createHighValueCustomer() {
            CustomerProfile profile = new CustomerProfile();
            profile.setCustomerId("test-customer");
            profile.setEmployeeId("EMP12345"); // High-value indicator
            profile.setAccountStatus("ACTIVE");
            profile.setSsn("123456789");
            return profile;
        }
        
        @Test
        @DisplayName("Should demonstrate real context-aware rule behavior")
        void shouldDemonstrateRealContextAwareRuleBehavior() {
            // Arrange - Use real rule implementations for integration testing
            String brand = "DIGITAL_BANK";
            List<String> ruleNames = Arrays.asList("TRUST_BASED_SECURITY_RULE", "HIGH_VALUE_CUSTOMER_RULE");
            Map<String, Integer> rulePriorities = new HashMap<>();
            rulePriorities.put("TRUST_BASED_SECURITY_RULE", 300);
            rulePriorities.put("HIGH_VALUE_CUSTOMER_RULE", 250);
            
            when(brandConfigService.getBrandConfiguration(brand)).thenReturn(digitalBankConfig);
            when(digitalBankConfig.getApplicableTokenSelectionRules()).thenReturn(ruleNames);
            when(digitalBankConfig.getRulePriorities()).thenReturn(rulePriorities);
            
            // Use real rule implementations
            TrustBasedSecurityRule realTrustRule = new TrustBasedSecurityRule();
            HighValueCustomerRule realHighValueRule = new HighValueCustomerRule();
            
            when(applicationContext.getBean("TRUST_BASED_SECURITY_RULE", TokenSelectionRule.class))
                .thenReturn(realTrustRule);
            when(applicationContext.getBean("HIGH_VALUE_CUSTOMER_RULE", TokenSelectionRule.class))
                .thenReturn(realHighValueRule);
            
            // Create test data
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createTestContext(redTrustInfo);
            CustomerProfile highValueCustomer = createHighValueCustomer();
            
            // Act - Get configured rules
            List<TokenSelectionRule> rules = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
            
            // Assert rules were loaded correctly
            assertNotNull(rules);
            assertEquals(2, rules.size());
            
            // Verify priority order (trust rule first due to higher priority)
            assertEquals(300, rules.get(0).getPriority()); // Trust rule
            assertEquals(250, rules.get(1).getPriority()); // High-value rule
            
            // Test rule execution with context awareness
            TokenSelectionRule priorityRule = rules.get(0); // Trust rule (highest priority)
            String selectedToken = priorityRule.determineNextToken(context, highValueCustomer);
            
            // For red trust level, should suggest SSN_FULL initially
            assertEquals("SSN_FULL", selectedToken, "Trust rule should suggest SSN_FULL for red trust");
            
            // Mark SSN_FULL as failed and test progression
            context.addFailedToken("SSN_FULL");
            String nextToken = priorityRule.determineNextToken(context, highValueCustomer);
            assertEquals("SSN", nextToken, "Trust rule should progress to SSN when SSN_FULL fails");
            
            // Mark all SSN variants as failed - rule should give up
            context.addFailedToken("SSN");
            context.addFailedToken("SSN_LAST_4");
            String exhaustedResult = priorityRule.determineNextToken(context, highValueCustomer);
            assertNull(exhaustedResult, "Trust rule should give up when all SSN variants fail");
            
            // Now high-value rule should take over
            TokenSelectionRule secondaryRule = rules.get(1); // High-value rule
            String highValueToken = secondaryRule.determineNextToken(context, highValueCustomer);
            assertEquals("MOBILE_PIN", highValueToken, "High-value rule should suggest MOBILE_PIN");
        }
        
        @Test
        @DisplayName("Should demonstrate anti-infinite-loop behavior")
        void shouldDemonstrateAntiInfiniteLoopBehavior() {
            // Arrange
            String brand = "TEST_BANK";
            List<String> ruleNames = Arrays.asList("TRUST_BASED_SECURITY_RULE");
            Map<String, Integer> rulePriorities = new HashMap<>();
            rulePriorities.put("TRUST_BASED_SECURITY_RULE", 300);
            
            when(brandConfigService.getBrandConfiguration(brand)).thenReturn(digitalBankConfig);
            when(digitalBankConfig.getApplicableTokenSelectionRules()).thenReturn(ruleNames);
            when(digitalBankConfig.getRulePriorities()).thenReturn(rulePriorities);
            
            TrustBasedSecurityRule realTrustRule = new TrustBasedSecurityRule();
            when(applicationContext.getBean("TRUST_BASED_SECURITY_RULE", TokenSelectionRule.class))
                .thenReturn(realTrustRule);
            
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createTestContext(redTrustInfo);
            CustomerProfile customer = createHighValueCustomer();
            
            // Act - Simulate multiple calls that would previously cause infinite loops
            List<TokenSelectionRule> rules = brandConfiguredRuleService.getTokenSelectionRulesForBrand(brand);
            TokenSelectionRule trustRule = rules.get(0);
            
            // Step 1: First call suggests SSN_FULL
            String call1 = trustRule.determineNextToken(context, customer);
            assertEquals("SSN_FULL", call1);
            
            // Step 2: Mark as failed, should progress to SSN
            context.addFailedToken("SSN_FULL");
            String call2 = trustRule.determineNextToken(context, customer);
            assertEquals("SSN", call2);
            
            // Step 3: Mark as failed, should progress to SSN_LAST_4
            context.addFailedToken("SSN");
            String call3 = trustRule.determineNextToken(context, customer);
            assertEquals("SSN_LAST_4", call3);
            
            // Step 4: Mark as failed, should give up (preventing infinite loop)
            context.addFailedToken("SSN_LAST_4");
            String call4 = trustRule.determineNextToken(context, customer);
            assertNull(call4, "Rule should give up to prevent infinite loops");
            
            // Step 5: Additional calls should continue to return null
            String call5 = trustRule.determineNextToken(context, customer);
            String call6 = trustRule.determineNextToken(context, customer);
            assertNull(call5, "Rule should consistently return null after exhaustion");
            assertNull(call6, "Rule should consistently return null after exhaustion");
            
            // Verify no infinite loop - failed tokens remain stable
            List<String> failedTokens = context.getFailedTokens();
            assertEquals(3, failedTokens.size(), "Should have exactly 3 failed tokens");
            assertTrue(failedTokens.contains("SSN_FULL"));
            assertTrue(failedTokens.contains("SSN"));
            assertTrue(failedTokens.contains("SSN_LAST_4"));
        }
        
        @Test
        @DisplayName("Should verify brand-agnostic rule reuse")
        void shouldVerifyBrandAgnosticRuleReuse() {
            // Arrange - Test that same rule can be used by different brands
            String digitalBrand = "DIGITAL_BANK";
            String premiumBrand = "PREMIUM_BANK";
            
            List<String> sharedRules = Arrays.asList("HIGH_VALUE_CUSTOMER_RULE");
            Map<String, Integer> digitalPriorities = new HashMap<>();
            digitalPriorities.put("HIGH_VALUE_CUSTOMER_RULE", 250);
            Map<String, Integer> premiumPriorities = new HashMap<>();
            premiumPriorities.put("HIGH_VALUE_CUSTOMER_RULE", 400); // Different priority
            
            // Create separate mock configurations for each brand
            BrandAuthConfiguration premiumConfig = org.mockito.Mockito.mock(BrandAuthConfiguration.class);
            
            when(brandConfigService.getBrandConfiguration(digitalBrand)).thenReturn(digitalBankConfig);
            when(brandConfigService.getBrandConfiguration(premiumBrand)).thenReturn(premiumConfig);
            
            // Configure digital bank mock
            when(digitalBankConfig.getApplicableTokenSelectionRules()).thenReturn(sharedRules);
            when(digitalBankConfig.getRulePriorities()).thenReturn(digitalPriorities);
            
            // Configure premium bank mock
            when(premiumConfig.getApplicableTokenSelectionRules()).thenReturn(sharedRules);
            when(premiumConfig.getRulePriorities()).thenReturn(premiumPriorities);
            
            HighValueCustomerRule sharedRule = new HighValueCustomerRule();
            when(applicationContext.getBean("HIGH_VALUE_CUSTOMER_RULE", TokenSelectionRule.class))
                .thenReturn(sharedRule);
            
            // Act
            List<TokenSelectionRule> digitalRules = brandConfiguredRuleService.getTokenSelectionRulesForBrand(digitalBrand);
            List<TokenSelectionRule> premiumRules = brandConfiguredRuleService.getTokenSelectionRulesForBrand(premiumBrand);
            
            // Assert - Same rule instance, different priorities
            assertNotNull(digitalRules);
            assertNotNull(premiumRules);
            assertEquals(1, digitalRules.size());
            assertEquals(1, premiumRules.size());
            
            // Verify different priorities applied
            assertEquals(250, digitalRules.get(0).getPriority(), "Digital Bank should have priority 250");
            assertEquals(400, premiumRules.get(0).getPriority(), "Premium Bank should have priority 400");
            
            // Verify brand-agnostic behavior
            assertEquals("DEFAULT", sharedRule.getBrand(), "Rule should be brand-agnostic");
            assertEquals("HIGH_VALUE_CUSTOMER_RULE", sharedRule.getRuleName(), "Rule should have correct bean name");
        }
    }
} 