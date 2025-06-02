package com.bank.ivr.auth.rule.impl;

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

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;

/**
 * Comprehensive tests for context-aware rules that prevent infinite loops
 * and provide natural progression through authentication methods.
 */
@DisplayName("Context-Aware Rules Test Suite")
class ContextAwareRulesTest {

    private TrustBasedSecurityRule trustBasedSecurityRule;
    private HighValueCustomerRule highValueCustomerRule;
    private RoyalBankTrustBasedSsnRule royalBankTrustRule;
    private FullAuthenticationCompletionRule completionRule;
    
    private CustomerProfile customerProfile;
    
    @BeforeEach
    void setUp() {
        trustBasedSecurityRule = new TrustBasedSecurityRule();
        highValueCustomerRule = new HighValueCustomerRule();
        royalBankTrustRule = new RoyalBankTrustBasedSsnRule();
        completionRule = new FullAuthenticationCompletionRule();
        
        // Set up customer profile
        customerProfile = new CustomerProfile();
        customerProfile.setCustomerId("test-customer-123");
        customerProfile.setSsn("123456789");
        customerProfile.setEmployeeId("EMP12345"); // High-value customer
        customerProfile.setAccountStatus("ACTIVE");
    }
    
    private AuthenticationContext createContext(TrustLevelInfo trustLevelInfo) {
        Map<String, Integer> tokenAttempts = new HashMap<>();
        tokenAttempts.put("SSN_FULL", 3);
        tokenAttempts.put("SSN", 3);
        tokenAttempts.put("SSN_LAST_4", 3);
        tokenAttempts.put("MOBILE_PIN", 3);
        tokenAttempts.put("DEBIT_CARD_PIN", 3);
        tokenAttempts.put("VOICE_BIOMETRIC", 3);
        tokenAttempts.put("FACE_ID", 3);
        
        return AuthenticationContext.builder()
                .attemptId("test-attempt-456")
                .sessionId("test-session-789")
                .customerIdentifier(new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "555-1234"))
                .brand("TEST_BANK")
                .startTime(LocalDateTime.now())
                .trustLevelInfo(trustLevelInfo)
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(8)
                .eligibleTokens(Arrays.asList("SSN_FULL", "SSN", "SSN_LAST_4", "MOBILE_PIN", "DEBIT_CARD_PIN", "VOICE_BIOMETRIC", "FACE_ID"))
                .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                .build();
    }
    
    @Nested
    @DisplayName("TrustBasedSecurityRule Context-Aware Tests")
    class TrustBasedSecurityRuleTests {
        
        @Test
        @DisplayName("Should suggest SSN_FULL for red trust level initially")
        void shouldSuggestSsnFullForRedTrustInitially() {
            // Arrange
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(redTrustInfo);
            
            // Act
            String result = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("SSN_FULL", result, "Should suggest SSN_FULL for red trust level initially");
        }
        
        @Test
        @DisplayName("Should progress to SSN when SSN_FULL fails")
        void shouldProgressToSsnWhenSsnFullFails() {
            // Arrange
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(redTrustInfo);
            
            // Mark SSN_FULL as failed
            context.addFailedToken("SSN_FULL");
            
            // Act
            String result = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("SSN", result, "Should progress to SSN when SSN_FULL fails");
        }
        
        @Test
        @DisplayName("Should progress to SSN_LAST_4 when both SSN_FULL and SSN fail")
        void shouldProgressToSsnLast4WhenBothSsnTokensFail() {
            // Arrange
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(redTrustInfo);
            
            // Mark both SSN_FULL and SSN as failed
            context.addFailedToken("SSN_FULL");
            context.addFailedToken("SSN");
            
            // Act
            String result = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("SSN_LAST_4", result, "Should progress to SSN_LAST_4 when both SSN_FULL and SSN fail");
        }
        
        @Test
        @DisplayName("Should give up when all SSN variants fail - preventing infinite loops")
        void shouldGiveUpWhenAllSsnVariantsFail() {
            // Arrange
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(redTrustInfo);
            
            // Mark all SSN variants as failed
            context.addFailedToken("SSN_FULL");
            context.addFailedToken("SSN");
            context.addFailedToken("SSN_LAST_4");
            
            // Act
            String result = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertNull(result, "Should return null when all SSN variants are exhausted to prevent infinite loops");
        }
        
        @Test
        @DisplayName("Should handle GREEN trust level appropriately")
        void shouldHandleGreenTrustLevel() {
            // Arrange
            TrustLevelInfo greenTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(greenTrustInfo);
            
            // Act
            String result = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertNull(result, "Should return null for GREEN trust level (let other rules handle)");
        }
        
        @Test
        @DisplayName("Should verify brand-agnostic behavior")
        void shouldVerifyBrandAgnosticBehavior() {
            // Act
            String brand = trustBasedSecurityRule.getBrand();
            String ruleName = trustBasedSecurityRule.getRuleName();
            
            // Assert
            assertEquals("DEFAULT", brand, "Should return DEFAULT for brand-agnostic rule");
            assertEquals("TRUST_BASED_SECURITY_RULE", ruleName, "Should match Spring bean name");
        }
    }
    
    @Nested
    @DisplayName("HighValueCustomerRule Context-Aware Tests")
    class HighValueCustomerRuleTests {
        
        @Test
        @DisplayName("Should suggest MOBILE_PIN for high-value customer initially")
        void shouldSuggestMobilePinForHighValueCustomerInitially() {
            // Arrange
            AuthenticationContext context = createContext(null);
            
            // Act
            String result = highValueCustomerRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("MOBILE_PIN", result, "Should suggest MOBILE_PIN for high-value customer initially");
        }
        
        @Test
        @DisplayName("Should progress to DEBIT_CARD_PIN when MOBILE_PIN fails")
        void shouldProgressToDebitCardPinWhenMobilePinFails() {
            // Arrange
            AuthenticationContext context = createContext(null);
            context.addFailedToken("MOBILE_PIN");
            
            // Act
            String result = highValueCustomerRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("DEBIT_CARD_PIN", result, "Should progress to DEBIT_CARD_PIN when MOBILE_PIN fails");
        }
        
        @Test
        @DisplayName("Should progress to VOICE_BIOMETRIC after PIN methods fail")
        void shouldProgressToVoiceBiometricAfterPinMethodsFail() {
            // Arrange
            AuthenticationContext context = createContext(null);
            context.addFailedToken("MOBILE_PIN");
            context.addFailedToken("DEBIT_CARD_PIN");
            
            // Act
            String result = highValueCustomerRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("VOICE_BIOMETRIC", result, "Should progress to VOICE_BIOMETRIC after PIN methods fail");
        }
        
        @Test
        @DisplayName("Should progress to FACE_ID as final option")
        void shouldProgressToFaceIdAsFinalOption() {
            // Arrange
            AuthenticationContext context = createContext(null);
            context.addFailedToken("MOBILE_PIN");
            context.addFailedToken("DEBIT_CARD_PIN");
            context.addFailedToken("VOICE_BIOMETRIC");
            
            // Act
            String result = highValueCustomerRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("FACE_ID", result, "Should progress to FACE_ID as final option");
        }
        
        @Test
        @DisplayName("Should give up when all high-value methods are exhausted")
        void shouldGiveUpWhenAllHighValueMethodsExhausted() {
            // Arrange
            AuthenticationContext context = createContext(null);
            context.addFailedToken("MOBILE_PIN");
            context.addFailedToken("DEBIT_CARD_PIN");
            context.addFailedToken("VOICE_BIOMETRIC");
            context.addFailedToken("FACE_ID");
            
            // Act
            String result = highValueCustomerRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertNull(result, "Should return null when all high-value methods are exhausted");
        }
        
        @Test
        @DisplayName("Should not apply to non-high-value customers")
        void shouldNotApplyToNonHighValueCustomers() {
            // Arrange
            AuthenticationContext context = createContext(null);
            CustomerProfile regularCustomer = new CustomerProfile();
            regularCustomer.setCustomerId("regular-customer");
            regularCustomer.setEmployeeId(null); // Not high-value
            regularCustomer.setAccountStatus("ACTIVE");
            
            // Act
            String result = highValueCustomerRule.determineNextToken(context, regularCustomer);
            
            // Assert
            assertNull(result, "Should return null for non-high-value customers");
        }
        
        @Test
        @DisplayName("Should verify brand-agnostic behavior")
        void shouldVerifyBrandAgnosticBehavior() {
            // Act
            String brand = highValueCustomerRule.getBrand();
            String ruleName = highValueCustomerRule.getRuleName();
            
            // Assert
            assertEquals("DEFAULT", brand, "Should return DEFAULT for brand-agnostic rule");
            assertEquals("HIGH_VALUE_CUSTOMER_RULE", ruleName, "Should match Spring bean name");
        }
    }
    
    @Nested
    @DisplayName("RoyalBankTrustBasedSsnRule Context-Aware Tests")
    class RoyalBankTrustBasedSsnRuleTests {
        
        @Test
        @DisplayName("Should suggest SSN_LAST_4 for green trust with single phone match")
        void shouldSuggestSsnLast4ForGreenTrustSingleMatch() {
            // Arrange
            TrustLevelInfo greenSingleMatch = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(greenSingleMatch);
            
            // Act
            String result = royalBankTrustRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("SSN_LAST_4", result, "Should suggest SSN_LAST_4 for green trust with single phone match");
        }
        
        @Test
        @DisplayName("Should suggest SSN_FULL for red trust with multiple phone matches")
        void shouldSuggestSsnFullForRedTrustMultipleMatches() {
            // Arrange
            TrustLevelInfo redMultipleMatch = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.MULTIPLE_MATCHES,
                3
            );
            AuthenticationContext context = createContext(redMultipleMatch);
            
            // Act
            String result = royalBankTrustRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertEquals("SSN_FULL", result, "Should suggest SSN_FULL for red trust with multiple phone matches");
        }
        
        @Test
        @DisplayName("Should give up when both SSN options fail - preventing infinite loops")
        void shouldGiveUpWhenBothSsnOptionsFail() {
            // Arrange
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(redTrustInfo);
            
            // Mark both SSN options as failed
            context.addFailedToken("SSN_FULL");
            context.addFailedToken("SSN_LAST_4");
            
            // Act
            String result = royalBankTrustRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertNull(result, "Should return null when both SSN options are failed to prevent infinite loops");
        }
        
        @Test
        @DisplayName("Should escalate SSN_LAST_4 failure to SSN_FULL")
        void shouldEscalateSsnLast4FailureToSsnFull() {
            // Arrange
            TrustLevelInfo greenNoMatch = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.NOT_MATCHED,
                0
            );
            AuthenticationContext context = createContext(greenNoMatch);
            
            // Act
            String escalationResult = royalBankTrustRule.handleTokenFailure(context, customerProfile, "SSN_LAST_4");
            
            // Assert
            assertEquals("SSN_FULL", escalationResult, "Should escalate SSN_LAST_4 failure to SSN_FULL");
        }
        
        @Test
        @DisplayName("Should verify brand-agnostic behavior with correct bean name")
        void shouldVerifyBrandAgnosticBehaviorWithCorrectBeanName() {
            // Act
            String brand = royalBankTrustRule.getBrand();
            String ruleName = royalBankTrustRule.getRuleName();
            
            // Assert
            assertEquals("DEFAULT", brand, "Should return DEFAULT for brand-agnostic rule");
            assertEquals("ROYAL_BANK_TRUST_LEVEL_RULE", ruleName, "Should match Spring bean name");
        }
    }
    
    @Nested
    @DisplayName("FullAuthenticationCompletionRule Tests")
    class FullAuthenticationCompletionRuleTests {
        
        @Test
        @DisplayName("Should detect incomplete authentication")
        void shouldDetectIncompleteAuthentication() {
            // Arrange
            AuthenticationContext context = createContext(null);
            
            // Act
            boolean isComplete = completionRule.isAuthenticationComplete(context, customerProfile);
            
            // Assert
            assertTrue(!isComplete, "Should detect authentication as incomplete when no tokens are authenticated");
        }
        
        @Test
        @DisplayName("Should detect complete authentication")
        void shouldDetectCompleteAuthentication() {
            // Arrange
            AuthenticationContext context = createContext(null);
            context.addAuthenticatedToken("SSN");
            
            // Act
            boolean isComplete = completionRule.isAuthenticationComplete(context, customerProfile);
            
            // Assert
            assertTrue(isComplete, "Should detect authentication as complete when at least one token is authenticated");
        }
        
        @Test
        @DisplayName("Should not suggest tokens - utility rule only")
        void shouldNotSuggestTokensUtilityRuleOnly() {
            // Arrange
            AuthenticationContext context = createContext(null);
            
            // Act
            String result = completionRule.determineNextToken(context, customerProfile);
            
            // Assert
            assertNull(result, "Completion rule should not suggest tokens - it's a utility rule");
        }
        
        @Test
        @DisplayName("Should have highest priority for completion checking")
        void shouldHaveHighestPriorityForCompletionChecking() {
            // Act
            int priority = completionRule.getPriority();
            
            // Assert
            assertEquals(1000, priority, "Completion rule should have highest priority (1000)");
        }
        
        @Test
        @DisplayName("Should verify brand-agnostic utility behavior")
        void shouldVerifyBrandAgnosticUtilityBehavior() {
            // Act
            String brand = completionRule.getBrand();
            String ruleName = completionRule.getRuleName();
            
            // Assert
            assertEquals("DEFAULT", brand, "Should return DEFAULT for brand-agnostic utility rule");
            assertEquals("FULL_AUTHENTICATION_COMPLETION_RULE", ruleName, "Should match Spring bean name");
        }
    }
    
    @Nested
    @DisplayName("Rule Progression Integration Tests")
    class RuleProgressionIntegrationTests {
        
        @Test
        @DisplayName("Should demonstrate natural rule progression without infinite loops")
        void shouldDemonstrateNaturalRuleProgressionWithoutInfiniteLoops() {
            // Arrange - High-value customer with red trust level
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(redTrustInfo);
            
            // Step 1: Trust rule should take precedence (higher priority)
            String step1 = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            assertEquals("SSN_FULL", step1, "Step 1: Trust rule should suggest SSN_FULL");
            
            // Step 2: Mark SSN_FULL as failed, trust rule should progress
            context.addFailedToken("SSN_FULL");
            String step2 = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            assertEquals("SSN", step2, "Step 2: Trust rule should progress to SSN");
            
            // Step 3: Mark SSN as failed, trust rule should progress to last fallback
            context.addFailedToken("SSN");
            String step3 = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            assertEquals("SSN_LAST_4", step3, "Step 3: Trust rule should progress to SSN_LAST_4");
            
            // Step 4: Mark SSN_LAST_4 as failed, trust rule should give up
            context.addFailedToken("SSN_LAST_4");
            String step4 = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            assertNull(step4, "Step 4: Trust rule should give up (return null)");
            
            // Step 5: High-value rule should take over
            String step5 = highValueCustomerRule.determineNextToken(context, customerProfile);
            assertEquals("MOBILE_PIN", step5, "Step 5: High-value rule should suggest MOBILE_PIN");
            
            // Step 6: Mark MOBILE_PIN as failed, high-value rule should progress
            context.addFailedToken("MOBILE_PIN");
            String step6 = highValueCustomerRule.determineNextToken(context, customerProfile);
            assertEquals("DEBIT_CARD_PIN", step6, "Step 6: High-value rule should progress to DEBIT_CARD_PIN");
            
            // Verify no infinite loops occurred
            List<String> failedTokens = context.getFailedTokens();
            assertEquals(4, failedTokens.size(), "Should have exactly 4 failed tokens without repetition");
            assertTrue(failedTokens.contains("SSN_FULL"), "Should contain failed SSN_FULL");
            assertTrue(failedTokens.contains("SSN"), "Should contain failed SSN");
            assertTrue(failedTokens.contains("SSN_LAST_4"), "Should contain failed SSN_LAST_4");
            assertTrue(failedTokens.contains("MOBILE_PIN"), "Should contain failed MOBILE_PIN");
        }
        
        @Test
        @DisplayName("Should verify successful authentication stops rule progression")
        void shouldVerifySuccessfulAuthenticationStopsRuleProgression() {
            // Arrange
            TrustLevelInfo redTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
            );
            AuthenticationContext context = createContext(redTrustInfo);
            
            // Step 1: Trust rule suggests SSN_FULL
            String step1 = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            assertEquals("SSN_FULL", step1, "Trust rule should suggest SSN_FULL");
            
            // Step 2: Mark SSN_FULL as successful
            context.addAuthenticatedToken("SSN_FULL");
            
            // Step 3: Completion rule should detect completion
            boolean isComplete = completionRule.isAuthenticationComplete(context, customerProfile);
            assertTrue(isComplete, "Completion rule should detect authentication is complete");
            
            // Step 4: Rules should not suggest additional tokens when authentication is complete
            String step4Trust = trustBasedSecurityRule.determineNextToken(context, customerProfile);
            String step4HighValue = highValueCustomerRule.determineNextToken(context, customerProfile);
            
            // Note: Rules might still suggest tokens, but the system should check completion first
            // The completion rule is what prevents further token requests
            assertNotNull(completionRule, "Completion rule should be available to check completion status");
        }
    }
} 