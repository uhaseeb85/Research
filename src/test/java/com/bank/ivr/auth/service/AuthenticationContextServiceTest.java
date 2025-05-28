package com.bank.ivr.auth.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.repository.AuthenticationContextRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationContextService Tests")
class AuthenticationContextServiceTest {

    @Mock
    private AuthenticationContextRepository contextRepository;

    @Mock
    private EligibilityService eligibilityService;

    @Mock
    private BrandAuthConfigurationService brandConfigService;

    @InjectMocks
    private AuthenticationContextService authenticationContextService;

    private AuthenticationRequest request;
    private CustomerProfile customerProfile;
    private List<AuthTokenDefinition> tokenDefinitions;

    @BeforeEach
    void setUp() {
        // Set up customer identifier
        CustomerIdentifier customerIdentifier = new CustomerIdentifier(
                CustomerIdentifier.IdentifierType.PHONE_NUMBER, "+1234567890");

        // Set up trust level info
        TrustLevelInfo trustLevelInfo = new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
        );

        // Set up authentication request
        request = new AuthenticationRequest(
                "session-123",
                customerIdentifier,
                null,
                null,
                "TEST_BANK",
                trustLevelInfo,
                null, // dnis
                null  // sessionSsn
        );

        // Set up customer profile
        customerProfile = CustomerProfile.builder()
                .customerId("CUST001")
                .phoneNumber("+1234567890")
                .accountStatus("ACTIVE")
                .build();

        // Set up token definitions
        AuthTokenDefinition ssnToken = AuthTokenDefinition.builder()
                .name("SSN")
                .description("Social Security Number")
                .priority(100)
                .maxAttempts(3)
                .build();

        AuthTokenDefinition pinToken = AuthTokenDefinition.builder()
                .name("DEBIT_CARD_PIN")
                .description("4-digit PIN")
                .priority(90)
                .maxAttempts(2)
                .build();

        tokenDefinitions = Arrays.asList(ssnToken, pinToken);
    }

    @Test
    @DisplayName("Should create initial context with brand-aware configuration")
    void shouldCreateInitialContextWithBrandAwareConfiguration() {
        // Given
        String attemptId = "attempt-123";
        List<String> eligibleTokens = Arrays.asList("SSN", "DEBIT_CARD_PIN");
        Map<String, Integer> brandSpecificAttempts = new HashMap<>();
        brandSpecificAttempts.put("SSN", 2); // Override default 3 attempts
        int maxOverallAttempts = 4;

        when(eligibilityService.determineEligibleTokens(customerProfile, "TEST_BANK"))
                .thenReturn(eligibleTokens);
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK"))
                .thenReturn(tokenDefinitions);
        when(brandConfigService.getBrandSpecificTokenAttempts("TEST_BANK"))
                .thenReturn(brandSpecificAttempts);
        when(brandConfigService.getMaxOverallAttemptsForBrand("TEST_BANK"))
                .thenReturn(maxOverallAttempts);

        // When
        AuthenticationContext context = authenticationContextService.createInitialContext(
                attemptId, request, customerProfile);

        // Then
        assertNotNull(context);
        assertEquals(attemptId, context.getAttemptId());
        assertEquals("session-123", context.getSessionId());
        assertEquals("TEST_BANK", context.getBrand());
        assertEquals(AuthStatus.PENDING_PRIMARY_TOKEN, context.getCurrentStatus());

        // Verify token state
        assertEquals(eligibleTokens, context.getEligibleTokens());
        assertTrue(context.getAuthenticatedTokens().isEmpty());
        assertTrue(context.getFailedTokens().isEmpty());
        assertTrue(context.getAskedTokens().isEmpty());

        // Verify attempt state with brand-specific overrides
        assertEquals(2, context.getTokenAttemptsRemaining().get("SSN")); // Brand override
        assertEquals(2, context.getTokenAttemptsRemaining().get("DEBIT_CARD_PIN")); // Token default
        assertEquals(maxOverallAttempts, context.getOverallAttemptsRemaining());

        // Verify session information
        assertEquals(request.getCustomerIdentifier(), context.getCustomerIdentifier());
        assertNotNull(context.getStartTime());
    }

    @Test
    @DisplayName("Should use token definition defaults when no brand-specific overrides")
    void shouldUseTokenDefinitionDefaultsWhenNoBrandSpecificOverrides() {
        // Given
        String attemptId = "attempt-456";
        List<String> eligibleTokens = Arrays.asList("SSN", "DEBIT_CARD_PIN");
        Map<String, Integer> emptyBrandSpecificAttempts = new HashMap<>(); // No overrides
        int maxOverallAttempts = 5;

        when(eligibilityService.determineEligibleTokens(customerProfile, "TEST_BANK"))
                .thenReturn(eligibleTokens);
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK"))
                .thenReturn(tokenDefinitions);
        when(brandConfigService.getBrandSpecificTokenAttempts("TEST_BANK"))
                .thenReturn(emptyBrandSpecificAttempts);
        when(brandConfigService.getMaxOverallAttemptsForBrand("TEST_BANK"))
                .thenReturn(maxOverallAttempts);

        // When
        AuthenticationContext context = authenticationContextService.createInitialContext(
                attemptId, request, customerProfile);

        // Then
        assertNotNull(context);
        
        // Should use token definition defaults
        assertEquals(3, context.getTokenAttemptsRemaining().get("SSN")); // Token default
        assertEquals(2, context.getTokenAttemptsRemaining().get("DEBIT_CARD_PIN")); // Token default
        assertEquals(maxOverallAttempts, context.getOverallAttemptsRemaining());
    }

    @Test
    @DisplayName("Should only include eligible tokens in attempt calculations")
    void shouldOnlyIncludeEligibleTokensInAttemptCalculations() {
        // Given
        String attemptId = "attempt-789";
        List<String> eligibleTokens = Arrays.asList("SSN"); // Only SSN is eligible
        Map<String, Integer> brandSpecificAttempts = new HashMap<>();

        when(eligibilityService.determineEligibleTokens(customerProfile, "TEST_BANK"))
                .thenReturn(eligibleTokens);
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK"))
                .thenReturn(tokenDefinitions);
        when(brandConfigService.getBrandSpecificTokenAttempts("TEST_BANK"))
                .thenReturn(brandSpecificAttempts);
        when(brandConfigService.getMaxOverallAttemptsForBrand("TEST_BANK"))
                .thenReturn(3);

        // When
        AuthenticationContext context = authenticationContextService.createInitialContext(
                attemptId, request, customerProfile);

        // Then
        assertNotNull(context);
        
        // Should only have attempts for eligible tokens
        assertEquals(1, context.getTokenAttemptsRemaining().size());
        assertTrue(context.getTokenAttemptsRemaining().containsKey("SSN"));
        assertFalse(context.getTokenAttemptsRemaining().containsKey("DEBIT_CARD_PIN"));
        
        // Eligible tokens should match
        assertEquals(Arrays.asList("SSN"), context.getEligibleTokens());
    }

    @Test
    @DisplayName("Should retrieve context by attempt ID")
    void shouldRetrieveContextByAttemptId() {
        // Given
        String attemptId = "attempt-123";
        AuthenticationContext expectedContext = AuthenticationContext.builder()
                .attemptId(attemptId)
                .build();

        when(contextRepository.findByAttemptId(attemptId))
                .thenReturn(Optional.of(expectedContext));

        // When
        Optional<AuthenticationContext> result = authenticationContextService.getContextByAttemptId(attemptId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedContext, result.get());
        verify(contextRepository).findByAttemptId(attemptId);
    }

    @Test
    @DisplayName("Should return empty when context not found")
    void shouldReturnEmptyWhenContextNotFound() {
        // Given
        String attemptId = "non-existent-attempt";

        when(contextRepository.findByAttemptId(attemptId))
                .thenReturn(Optional.empty());

        // When
        Optional<AuthenticationContext> result = authenticationContextService.getContextByAttemptId(attemptId);

        // Then
        assertFalse(result.isPresent());
        verify(contextRepository).findByAttemptId(attemptId);
    }

    @Test
    @DisplayName("Should save context")
    void shouldSaveContext() {
        // Given
        AuthenticationContext context = AuthenticationContext.builder()
                .attemptId("attempt-123")
                .build();

        // When
        authenticationContextService.saveContext(context);

        // Then
        verify(contextRepository).save(context);
    }

    @Test
    @DisplayName("Should update context")
    void shouldUpdateContext() {
        // Given
        AuthenticationContext context = AuthenticationContext.builder()
                .attemptId("attempt-123")
                .build();

        // When
        authenticationContextService.updateContext(context);

        // Then
        verify(contextRepository).update(context);
    }

    @Test
    @DisplayName("Should delete context by attempt ID")
    void shouldDeleteContextByAttemptId() {
        // Given
        String attemptId = "attempt-123";

        // When
        authenticationContextService.deleteContext(attemptId);

        // Then
        verify(contextRepository).deleteByAttemptId(attemptId);
    }

    @Test
    @DisplayName("Should generate unique attempt ID")
    void shouldGenerateUniqueAttemptId() {
        // When
        String attemptId1 = authenticationContextService.generateAttemptId();
        String attemptId2 = authenticationContextService.generateAttemptId();

        // Then
        assertNotNull(attemptId1);
        assertNotNull(attemptId2);
        assertNotEquals(attemptId1, attemptId2);
        assertTrue(attemptId1.length() > 0);
        assertTrue(attemptId2.length() > 0);
    }

    @Test
    @DisplayName("Should handle empty eligible tokens list")
    void shouldHandleEmptyEligibleTokensList() {
        // Given
        String attemptId = "attempt-empty";
        List<String> emptyEligibleTokens = Arrays.asList(); // No eligible tokens
        Map<String, Integer> brandSpecificAttempts = new HashMap<>();

        when(eligibilityService.determineEligibleTokens(customerProfile, "TEST_BANK"))
                .thenReturn(emptyEligibleTokens);
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK"))
                .thenReturn(tokenDefinitions);
        when(brandConfigService.getBrandSpecificTokenAttempts("TEST_BANK"))
                .thenReturn(brandSpecificAttempts);
        when(brandConfigService.getMaxOverallAttemptsForBrand("TEST_BANK"))
                .thenReturn(3);

        // When
        AuthenticationContext context = authenticationContextService.createInitialContext(
                attemptId, request, customerProfile);

        // Then
        assertNotNull(context);
        assertTrue(context.getEligibleTokens().isEmpty());
        assertTrue(context.getTokenAttemptsRemaining().isEmpty());
        assertEquals(3, context.getOverallAttemptsRemaining());
    }

    @Test
    @DisplayName("Should log brand-aware context creation")
    void shouldLogBrandAwareContextCreation() {
        // Given
        String attemptId = "attempt-logging";
        List<String> eligibleTokens = Arrays.asList("SSN");
        Map<String, Integer> brandSpecificAttempts = new HashMap<>();

        when(eligibilityService.determineEligibleTokens(customerProfile, "TEST_BANK"))
                .thenReturn(eligibleTokens);
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK"))
                .thenReturn(tokenDefinitions);
        when(brandConfigService.getBrandSpecificTokenAttempts("TEST_BANK"))
                .thenReturn(brandSpecificAttempts);
        when(brandConfigService.getMaxOverallAttemptsForBrand("TEST_BANK"))
                .thenReturn(3);

        // When
        AuthenticationContext context = authenticationContextService.createInitialContext(
                attemptId, request, customerProfile);

        // Then
        assertNotNull(context);
        // Note: In a real test, you might want to verify logging using a logging framework test utility
        // For now, we just verify the context was created successfully
        assertEquals("TEST_BANK", context.getBrand());
        assertEquals(attemptId, context.getAttemptId());
    }

    @Test
    @DisplayName("Should handle different brand configurations")
    void shouldHandleDifferentBrandConfigurations() {
        // Given - Different brand with different configuration
        AuthenticationRequest premiumBankRequest = new AuthenticationRequest(
                "session-premium",
                request.getCustomerIdentifier(),
                null,
                null,
                "PREMIUM_BANK",
                request.getTrustLevelInfo(),
                null, // dnis
                null  // sessionSsn
        );

        String attemptId = "attempt-premium";
        List<String> premiumEligibleTokens = Arrays.asList("SSN", "DEBIT_CARD_PIN");
        Map<String, Integer> premiumBrandSpecificAttempts = new HashMap<>();
        premiumBrandSpecificAttempts.put("SSN", 1); // Very strict
        premiumBrandSpecificAttempts.put("DEBIT_CARD_PIN", 1);
        int premiumMaxOverallAttempts = 2; // Very strict

        when(eligibilityService.determineEligibleTokens(customerProfile, "PREMIUM_BANK"))
                .thenReturn(premiumEligibleTokens);
        when(brandConfigService.getTokenDefinitionsForBrand("PREMIUM_BANK"))
                .thenReturn(tokenDefinitions);
        when(brandConfigService.getBrandSpecificTokenAttempts("PREMIUM_BANK"))
                .thenReturn(premiumBrandSpecificAttempts);
        when(brandConfigService.getMaxOverallAttemptsForBrand("PREMIUM_BANK"))
                .thenReturn(premiumMaxOverallAttempts);

        // When
        AuthenticationContext context = authenticationContextService.createInitialContext(
                attemptId, premiumBankRequest, customerProfile);

        // Then
        assertNotNull(context);
        assertEquals("PREMIUM_BANK", context.getBrand());
        
        // Verify strict premium bank configuration
        assertEquals(1, context.getTokenAttemptsRemaining().get("SSN"));
        assertEquals(1, context.getTokenAttemptsRemaining().get("DEBIT_CARD_PIN"));
        assertEquals(2, context.getOverallAttemptsRemaining());
    }
} 