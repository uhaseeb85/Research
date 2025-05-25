package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.BrandFailurePolicy;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test for brand-specific failure policy functionality.
 * Demonstrates how different brands handle authentication failures differently.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Brand Failure Policy Service Tests")
class BrandFailurePolicyServiceTest {

    @Mock
    private BrandAuthConfigurationService brandConfigService;

    @InjectMocks
    private BrandFailurePolicyService failurePolicyService;

    private AuthenticationContext context;
    private CustomerProfile customerProfile;
    private List<AuthTokenDefinition> tokenDefinitions;

    @BeforeEach
    void setUp() {
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
                .maxAttempts(3)
                .build();

        AuthTokenDefinition dobToken = AuthTokenDefinition.builder()
                .name("DATE_OF_BIRTH")
                .description("Date of Birth")
                .priority(80)
                .maxAttempts(3)
                .build();

        tokenDefinitions = Arrays.asList(ssnToken, pinToken, dobToken);

        // Set up customer profile
        customerProfile = new CustomerProfile();

        // Set up authentication context
        Map<String, Integer> tokenAttempts = new HashMap<>();
        tokenAttempts.put("SSN", 3);
        tokenAttempts.put("DEBIT_CARD_PIN", 3);
        tokenAttempts.put("DATE_OF_BIRTH", 3);

        context = AuthenticationContext.builder()
                .attemptId("test-attempt-123")
                .sessionId("test-session-456")
                .customerIdentifier(new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "555-1234"))
                .brand("TEST_BRAND")
                .startTime(LocalDateTime.now())
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(5)
                .eligibleTokens(Arrays.asList("SSN", "DEBIT_CARD_PIN", "DATE_OF_BIRTH"))
                .requiredTokensForFullAuth(Arrays.asList("SSN", "DEBIT_CARD_PIN"))
                .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                .build();
    }

    @Test
    @DisplayName("Premium Bank: Should fail immediately on critical token failure")
    void testPremiumBankCriticalTokenFailure() {
        // Given - Premium Bank policy with critical tokens
        BrandFailurePolicy premiumPolicy = BrandFailurePolicy.builder()
                .brandCode("PREMIUM_BANK")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.ALLOW_ALTERNATIVES)
                .criticalTokens(Arrays.asList("DEBIT_CARD_PIN"))
                .failOnCriticalTokenFailure(true)
                .build();

        when(brandConfigService.getBrandFailurePolicy("PREMIUM_BANK")).thenReturn(premiumPolicy);

        // When - Critical token (PIN) fails
        context.addFailedToken("DEBIT_CARD_PIN");

        // Then - Should fail immediately
        boolean shouldFail = failurePolicyService.shouldFailAuthentication(context, customerProfile, "PREMIUM_BANK");
        assertTrue(shouldFail, "Premium Bank should fail immediately when critical token fails");
    }

    @Test
    @DisplayName("Community Bank: Should allow all tokens to be attempted before failing")
    void testCommunityBankRequireAllAttempted() {
        // Given - Community Bank policy requiring all tokens to be attempted
        BrandFailurePolicy communityPolicy = BrandFailurePolicy.builder()
                .brandCode("COMMUNITY_BANK")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.REQUIRE_ALL_ATTEMPTED)
                .alternativeTokenStrategy(BrandFailurePolicy.AlternativeTokenStrategy.ANY_REMAINING)
                .build();

        when(brandConfigService.getBrandFailurePolicy("COMMUNITY_BANK")).thenReturn(communityPolicy);

        // When - Only one token has failed, others still available
        context.addFailedToken("SSN");

        // Then - Should not fail yet
        boolean shouldFail = failurePolicyService.shouldFailAuthentication(context, customerProfile, "COMMUNITY_BANK");
        assertFalse(shouldFail, "Community Bank should not fail until all tokens attempted");

        // When - All tokens have been attempted/failed
        context.addFailedToken("DEBIT_CARD_PIN");
        context.addFailedToken("DATE_OF_BIRTH");

        // Then - Should fail now
        shouldFail = failurePolicyService.shouldFailAuthentication(context, customerProfile, "COMMUNITY_BANK");
        assertTrue(shouldFail, "Community Bank should fail after all tokens attempted");
    }

    @Test
    @DisplayName("Should get predefined alternatives based on brand policy")
    void testPredefinedAlternatives() {
        // Given - Policy with predefined alternatives
        Map<String, List<String>> tokenAlternatives = new HashMap<>();
        tokenAlternatives.put("SSN", Arrays.asList("DATE_OF_BIRTH", "DEBIT_CARD_PIN"));
        tokenAlternatives.put("DEBIT_CARD_PIN", Arrays.asList("DATE_OF_BIRTH"));

        BrandFailurePolicy policy = BrandFailurePolicy.builder()
                .brandCode("TEST_BRAND")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.ALLOW_ALTERNATIVES)
                .alternativeTokenStrategy(BrandFailurePolicy.AlternativeTokenStrategy.PREDEFINED_ALTERNATIVES)
                .tokenAlternatives(tokenAlternatives)
                .build();

        when(brandConfigService.getBrandFailurePolicy("TEST_BRAND")).thenReturn(policy);

        // When - SSN fails
        context.addFailedToken("SSN");

        // Then - Should get predefined alternatives for SSN
        List<AuthTokenDefinition> alternatives = failurePolicyService.getAlternativeTokens(context, "TEST_BRAND", tokenDefinitions);
        
        assertEquals(2, alternatives.size(), "Should get 2 alternatives for SSN");
        assertTrue(alternatives.stream().anyMatch(t -> t.getName().equals("DATE_OF_BIRTH")), 
                  "Should include DATE_OF_BIRTH as alternative");
        assertTrue(alternatives.stream().anyMatch(t -> t.getName().equals("DEBIT_CARD_PIN")), 
                  "Should include DEBIT_CARD_PIN as alternative");
    }

    @Test
    @DisplayName("Should get priority-based alternatives")
    void testPriorityBasedAlternatives() {
        // Given - Policy with priority-based alternatives
        BrandFailurePolicy policy = BrandFailurePolicy.builder()
                .brandCode("TEST_BRAND")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.ALLOW_ALTERNATIVES)
                .alternativeTokenStrategy(BrandFailurePolicy.AlternativeTokenStrategy.PRIORITY_BASED)
                .build();

        when(brandConfigService.getBrandFailurePolicy("TEST_BRAND")).thenReturn(policy);

        // When - SSN fails
        context.addFailedToken("SSN");

        // Then - Should get remaining tokens in priority order
        List<AuthTokenDefinition> alternatives = failurePolicyService.getAlternativeTokens(context, "TEST_BRAND", tokenDefinitions);
        
        assertEquals(2, alternatives.size(), "Should get 2 remaining alternatives");
        // Should be ordered by priority (PIN=90, DOB=80)
        assertEquals("DEBIT_CARD_PIN", alternatives.get(0).getName(), "Highest priority alternative should be first");
        assertEquals("DATE_OF_BIRTH", alternatives.get(1).getName(), "Lower priority alternative should be second");
    }

    @Test
    @DisplayName("Should support group-based alternatives")
    void testGroupBasedAlternatives() {
        // Given - Policy with group-based alternatives
        Map<String, List<String>> tokenGroups = new HashMap<>();
        tokenGroups.put("PRIMARY_GROUP", Arrays.asList("SSN", "DEBIT_CARD_PIN"));
        tokenGroups.put("SECONDARY_GROUP", Arrays.asList("DATE_OF_BIRTH"));

        BrandFailurePolicy policy = BrandFailurePolicy.builder()
                .brandCode("TEST_BRAND")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.PROGRESSIVE_FALLBACK)
                .alternativeTokenStrategy(BrandFailurePolicy.AlternativeTokenStrategy.GROUP_BASED)
                .tokenGroups(tokenGroups)
                .fallbackGroups(Arrays.asList("PRIMARY_GROUP", "SECONDARY_GROUP"))
                .build();

        when(brandConfigService.getBrandFailurePolicy("TEST_BRAND")).thenReturn(policy);

        // When - SSN fails but PIN is still available in primary group
        context.addFailedToken("SSN");

        // Then - Should get PIN from primary group
        List<AuthTokenDefinition> alternatives = failurePolicyService.getAlternativeTokens(context, "TEST_BRAND", tokenDefinitions);
        
        assertEquals(1, alternatives.size(), "Should get 1 alternative from primary group");
        assertEquals("DEBIT_CARD_PIN", alternatives.get(0).getName(), "Should get PIN from primary group");

        // When - All primary group tokens fail
        context.addFailedToken("DEBIT_CARD_PIN");

        // Then - Should get tokens from secondary group
        alternatives = failurePolicyService.getAlternativeTokens(context, "TEST_BRAND", tokenDefinitions);
        
        assertEquals(1, alternatives.size(), "Should get 1 alternative from secondary group");
        assertEquals("DATE_OF_BIRTH", alternatives.get(0).getName(), "Should get DOB from secondary group");
    }

    @Test
    @DisplayName("Should support partial authentication")
    void testPartialAuthentication() {
        // Given - Policy allowing partial authentication
        BrandFailurePolicy policy = BrandFailurePolicy.builder()
                .brandCode("TEST_BRAND")
                .allowPartialAuthentication(true)
                .partialAuthMinTokens(1)
                .build();

        when(brandConfigService.getBrandFailurePolicy("TEST_BRAND")).thenReturn(policy);

        // When - One token is authenticated
        context.addAuthenticatedToken("DEBIT_CARD_PIN");

        // Then - Should allow partial authentication
        boolean allowed = failurePolicyService.isPartialAuthenticationAllowed(context, "TEST_BRAND");
        assertTrue(allowed, "Should allow partial authentication with 1 token");

        // When - Policy requires 2 tokens minimum
        policy = BrandFailurePolicy.builder()
                .brandCode("TEST_BRAND")
                .allowPartialAuthentication(true)
                .partialAuthMinTokens(2)
                .build();

        when(brandConfigService.getBrandFailurePolicy("TEST_BRAND")).thenReturn(policy);

        // Then - Should not allow partial authentication with only 1 token
        allowed = failurePolicyService.isPartialAuthenticationAllowed(context, "TEST_BRAND");
        assertFalse(allowed, "Should not allow partial authentication with insufficient tokens");
    }

    @Test
    @DisplayName("Should get next best alternative token")
    void testGetNextAlternativeToken() {
        // Given - Policy with alternatives
        Map<String, List<String>> tokenAlternatives = new HashMap<>();
        tokenAlternatives.put("SSN", Arrays.asList("DEBIT_CARD_PIN", "DATE_OF_BIRTH"));

        BrandFailurePolicy policy = BrandFailurePolicy.builder()
                .brandCode("TEST_BRAND")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.ALLOW_ALTERNATIVES)
                .alternativeTokenStrategy(BrandFailurePolicy.AlternativeTokenStrategy.PREDEFINED_ALTERNATIVES)
                .tokenAlternatives(tokenAlternatives)
                .build();

        when(brandConfigService.getBrandFailurePolicy("TEST_BRAND")).thenReturn(policy);

        // When - SSN fails
        context.addFailedToken("SSN");

        // Then - Should get highest priority alternative
        AuthTokenDefinition nextToken = failurePolicyService.getNextAlternativeToken(context, "TEST_BRAND", tokenDefinitions);
        
        assertNotNull(nextToken, "Should get an alternative token");
        assertEquals("DEBIT_CARD_PIN", nextToken.getName(), "Should get highest priority alternative (PIN=90 > DOB=80)");
    }

    @Test
    @DisplayName("Should handle no alternatives available")
    void testNoAlternativesAvailable() {
        // Given - Policy with no alternatives
        BrandFailurePolicy policy = BrandFailurePolicy.builder()
                .brandCode("TEST_BRAND")
                .failureStrategy(BrandFailurePolicy.FailureStrategy.FAIL_IMMEDIATELY)
                .build();

        when(brandConfigService.getBrandFailurePolicy("TEST_BRAND")).thenReturn(policy);

        // When - All tokens have failed
        context.addFailedToken("SSN");
        context.addFailedToken("DEBIT_CARD_PIN");
        context.addFailedToken("DATE_OF_BIRTH");

        // Then - Should get no alternatives
        AuthTokenDefinition nextToken = failurePolicyService.getNextAlternativeToken(context, "TEST_BRAND", tokenDefinitions);
        
        assertNull(nextToken, "Should get no alternative when all tokens failed");
    }
} 