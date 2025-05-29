package com.bank.ivr.auth.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.rule.PostValidationRule;
import com.bank.ivr.auth.rule.impl.TrustBasedAdditionalTokenRule;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostValidationRuleService Tests")
class PostValidationRuleServiceTest {

    @Mock
    private AuthenticationContext context;
    
    private PostValidationRuleService postValidationRuleService;
    private CustomerProfile customerProfile;
    private TrustBasedAdditionalTokenRule trustBasedRule;

    @BeforeEach
    void setUp() {
        trustBasedRule = new TrustBasedAdditionalTokenRule();
        List<PostValidationRule> rules = Arrays.asList(trustBasedRule);
        postValidationRuleService = new PostValidationRuleService(rules);
        
        // Set up customer profile
        customerProfile = CustomerProfile.builder()
                .customerId("CUST001")
                .phoneNumber("+1234567890")
                .accountNumber("ACC001")
                .ssn("123456789")
                .hashedPin("hashedPin123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .fullName("John Doe")
                .email("john.doe@email.com")
                .address("123 Main St")
                .accountStatus("ACTIVE")
                .build();
        
        // Set up basic context mocks
        when(context.getBrand()).thenReturn("TEST_BANK");
        when(context.getAttemptId()).thenReturn("attempt-123");
    }

    @Test
    @DisplayName("Should require additional tokens for low trust level (RED)")
    void shouldRequireAdditionalTokensForLowTrust() {
        // Given: Low trust level (RED)
        TrustLevelInfo lowTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
        );
        when(context.getTrustLevelInfo()).thenReturn(lowTrustInfo);

        // When: Evaluating post-validation for SSN_LAST_4
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "SSN_LAST_4", context, customerProfile);

        // Then: Should require additional tokens
        assertTrue(result.isValid(), "Result should be valid");
        assertTrue(result.requiresAdditionalTokens(), "Should require additional tokens for low trust");
        assertNotNull(result.getSuggestedAdditionalTokens(), "Should have suggested additional tokens");
        assertTrue(result.getSuggestedAdditionalTokens().contains("SSN_FULL"), "Should suggest full SSN");
        assertTrue(result.getSuggestedAdditionalTokens().contains("DEBIT_CARD_PIN"), "Should suggest PIN");
        assertEquals("HIGH", result.getRiskLevel(), "Risk level should be HIGH for low trust");
        assertNotNull(result.getReason(), "Should have a reason");
        assertTrue(result.getReason().contains("Low trust level detected"), "Reason should mention low trust");
    }

    @Test
    @DisplayName("Should require additional tokens for multiple phone matches")
    void shouldRequireAdditionalTokensForMultiplePhoneMatches() {
        // Given: High trust but multiple phone matches
        TrustLevelInfo multipleMatchInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.MULTIPLE_MATCHES,
                3
        );
        when(context.getTrustLevelInfo()).thenReturn(multipleMatchInfo);

        // When: Evaluating post-validation for DEBIT_CARD_PIN
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "DEBIT_CARD_PIN", context, customerProfile);

        // Then: Should require additional tokens
        assertTrue(result.isValid(), "Result should be valid");
        assertTrue(result.requiresAdditionalTokens(), "Should require additional tokens for multiple matches");
        assertNotNull(result.getSuggestedAdditionalTokens(), "Should have suggested additional tokens");
        assertTrue(result.getSuggestedAdditionalTokens().contains("SSN_FULL"), "Should suggest full SSN");
        assertTrue(result.getSuggestedAdditionalTokens().contains("DATE_OF_BIRTH"), "Should suggest DOB");
        assertEquals("MEDIUM", result.getRiskLevel(), "Risk level should be MEDIUM");
        assertTrue(result.getReason().contains("matches 3 customer accounts"), "Reason should mention multiple matches");
    }

    @Test
    @DisplayName("Should require additional tokens for phone not matched")
    void shouldRequireAdditionalTokensForPhoneNotMatched() {
        // Given: High trust but phone not matched
        TrustLevelInfo noMatchInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.NOT_MATCHED,
                0
        );
        when(context.getTrustLevelInfo()).thenReturn(noMatchInfo);

        // When: Evaluating post-validation for DEBIT_CARD_PIN
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "DEBIT_CARD_PIN", context, customerProfile);

        // Then: Should require additional tokens
        assertTrue(result.isValid(), "Result should be valid");
        assertTrue(result.requiresAdditionalTokens(), "Should require additional tokens for no phone match");
        assertNotNull(result.getSuggestedAdditionalTokens(), "Should have suggested additional tokens");
        assertTrue(result.getSuggestedAdditionalTokens().contains("SSN_LAST_4"), "Should suggest SSN last 4");
        assertTrue(result.getSuggestedAdditionalTokens().contains("ACCOUNT_NUMBER"), "Should suggest account number");
        assertEquals("MEDIUM", result.getRiskLevel(), "Risk level should be MEDIUM");
        assertTrue(result.getReason().contains("not found in our records"), "Reason should mention phone not found");
    }

    @Test
    @DisplayName("Should require additional tokens for high-value customer")
    void shouldRequireAdditionalTokensForHighValueCustomer() {
        // Given: High trust, single match, but employee account (high-value)
        TrustLevelInfo normalTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
        );
        when(context.getTrustLevelInfo()).thenReturn(normalTrustInfo);
        
        // Set up high-value customer (employee)
        CustomerProfile highValueCustomer = CustomerProfile.builder()
                .customerId("CUST001")
                .phoneNumber("+1234567890")
                .accountNumber("ACC001")
                .ssn("123456789")
                .hashedPin("hashedPin123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .fullName("John Doe")
                .email("john.doe@email.com")
                .address("123 Main St")
                .accountStatus("ACTIVE")
                .employeeId("EMP001") // This makes them high-value
                .build();

        // When: Evaluating post-validation for SSN_LAST_4 (not a strong token)
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "SSN_LAST_4", context, highValueCustomer);

        // Then: Should require additional tokens
        assertTrue(result.isValid(), "Result should be valid");
        assertTrue(result.requiresAdditionalTokens(), "Should require additional tokens for high-value customer");
        assertNotNull(result.getSuggestedAdditionalTokens(), "Should have suggested additional tokens");
        assertTrue(result.getSuggestedAdditionalTokens().contains("DEBIT_CARD_PIN"), "Should suggest PIN");
        assertTrue(result.getSuggestedAdditionalTokens().contains("DATE_OF_BIRTH"), "Should suggest DOB");
        assertEquals("MEDIUM", result.getRiskLevel(), "Risk level should be MEDIUM");
        assertTrue(result.getReason().contains("High-value customer"), "Reason should mention high-value customer");
    }

    @Test
    @DisplayName("Should NOT require additional tokens for normal scenario")
    void shouldNotRequireAdditionalTokensForNormalScenario() {
        // Given: High trust, single match, normal customer
        TrustLevelInfo normalTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
        );
        when(context.getTrustLevelInfo()).thenReturn(normalTrustInfo);

        // When: Evaluating post-validation for DEBIT_CARD_PIN
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "DEBIT_CARD_PIN", context, customerProfile);

        // Then: Should NOT require additional tokens
        assertTrue(result.isValid(), "Result should be valid");
        assertFalse(result.requiresAdditionalTokens(), "Should NOT require additional tokens for normal scenario");
        assertNull(result.getSuggestedAdditionalTokens(), "Should not have suggested additional tokens");
        assertNull(result.getReason(), "Should not have a reason");
        assertNull(result.getRiskLevel(), "Should not have a risk level");
    }

    @Test
    @DisplayName("Should NOT require additional tokens when no trust info available")
    void shouldNotRequireAdditionalTokensWhenNoTrustInfo() {
        // Given: No trust level info
        when(context.getTrustLevelInfo()).thenReturn(null);

        // When: Evaluating post-validation
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "SSN_LAST_4", context, customerProfile);

        // Then: Should NOT require additional tokens
        assertTrue(result.isValid(), "Result should be valid");
        assertFalse(result.requiresAdditionalTokens(), "Should NOT require additional tokens without trust info");
    }

    @Test
    @DisplayName("Should handle customer with risk indicators")
    void shouldRequireAdditionalTokensForRiskIndicators() {
        // Given: Normal trust but customer has risk indicators
        TrustLevelInfo normalTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
        );
        when(context.getTrustLevelInfo()).thenReturn(normalTrustInfo);
        
        // Set up customer with risk indicators (flagged account)
        CustomerProfile riskyCustomer = CustomerProfile.builder()
                .customerId("CUST001")
                .phoneNumber("+1234567890")
                .accountNumber("ACC001")
                .ssn("123456789")
                .hashedPin("hashedPin123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .fullName("John Doe")
                .email("john.doe@email.com")
                .address("123 Main St")
                .accountStatus("FLAGGED") // This triggers risk indicators
                .build();

        // When: Evaluating post-validation
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "DEBIT_CARD_PIN", context, riskyCustomer);

        // Then: Should require additional tokens
        assertTrue(result.isValid(), "Result should be valid");
        assertTrue(result.requiresAdditionalTokens(), "Should require additional tokens for risky customer");
        assertNotNull(result.getSuggestedAdditionalTokens(), "Should have suggested additional tokens");
        assertTrue(result.getSuggestedAdditionalTokens().contains("SSN_LAST_4"), "Should suggest SSN last 4");
        assertTrue(result.getSuggestedAdditionalTokens().contains("DEBIT_CARD_PIN"), "Should suggest PIN");
        assertEquals("MEDIUM", result.getRiskLevel(), "Risk level should be MEDIUM");
        assertTrue(result.getReason().contains("Recent account activity"), "Reason should mention account activity");
    }

    @Test
    @DisplayName("Should not apply rule when customer account is not active")
    void shouldNotApplyRuleWhenCustomerNotActive() {
        // Given: Trust info available but customer account is not active
        TrustLevelInfo lowTrustInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.RED,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
        );
        when(context.getTrustLevelInfo()).thenReturn(lowTrustInfo);
        
        CustomerProfile inactiveCustomer = CustomerProfile.builder()
                .customerId("CUST001")
                .phoneNumber("+1234567890")
                .accountNumber("ACC001")
                .ssn("123456789")
                .hashedPin("hashedPin123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .fullName("John Doe")
                .email("john.doe@email.com")
                .address("123 Main St")
                .accountStatus("INACTIVE") // Not active
                .build();

        // When: Evaluating post-validation
        TokenValidationResult result = postValidationRuleService.evaluatePostValidation(
                "SSN_LAST_4", context, inactiveCustomer);

        // Then: Should NOT require additional tokens (rule not applicable)
        assertTrue(result.isValid(), "Result should be valid");
        assertFalse(result.requiresAdditionalTokens(), "Should NOT require additional tokens for inactive customer");
    }
} 