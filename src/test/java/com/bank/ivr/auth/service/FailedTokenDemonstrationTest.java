package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.ProvidedToken;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
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
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive demonstration of failed token handling throughout the authentication flow.
 * This test shows how tokens that fail validation are properly tracked, excluded from 
 * future selection, and included in responses.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Failed Token Handling Demonstration")
class FailedTokenDemonstrationTest {

    @Mock
    private TokenValidationService tokenValidationService;
    
    @Mock
    private AuthenticationContextService contextService;
    
    @Mock
    private BrandAuthConfigurationService brandConfigService;

    @Mock
    private BrandFailurePolicyService failurePolicyService;

    @InjectMocks
    private TokenProcessingService tokenProcessingService;
    
    @InjectMocks
    private AuthenticationResponseService authenticationResponseService;

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
                .maxAttempts(2) // Only 2 attempts for this demo
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

        // Set up authentication context with limited attempts for SSN
        Map<String, Integer> tokenAttempts = new HashMap<>();
        tokenAttempts.put("SSN", 2); // Only 2 attempts
        tokenAttempts.put("DEBIT_CARD_PIN", 3);
        tokenAttempts.put("DATE_OF_BIRTH", 3);

        context = AuthenticationContext.builder()
                .attemptId("demo-attempt-123")
                .sessionId("demo-session-456")
                .customerIdentifier(new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "555-1234"))
                .brand("DEMO_BANK")
                .startTime(LocalDateTime.now())
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(5)
                .eligibleTokens(Arrays.asList("SSN", "DEBIT_CARD_PIN", "DATE_OF_BIRTH"))
                .requiredTokensForFullAuth(Arrays.asList("SSN", "DEBIT_CARD_PIN"))
                .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                .build();
        
        // Set up brand failure policy service mocks (lenient to avoid unnecessary stubbing errors)
        lenient().when(failurePolicyService.shouldFailAuthentication(any(), any(), any())).thenReturn(false);
        lenient().when(failurePolicyService.getNextAlternativeToken(any(), any(), any())).thenReturn(null);
        lenient().when(failurePolicyService.isPartialAuthenticationAllowed(any(), any())).thenReturn(false);
    }

    @Test
    @DisplayName("Complete Failed Token Flow Demonstration")
    void demonstrateCompleteFailedTokenFlow() {
        when(brandConfigService.getTokenDefinitionsForBrand("DEMO_BANK")).thenReturn(tokenDefinitions);
        
        // STEP 1: System asks for highest priority token (SSN)
        AuthenticationResponse response1 = authenticationResponseService.buildResponse(context, customerProfile, "DEMO_BANK");
        
        assertEquals("SSN", response1.getPrimaryTokenToAsk().getName(), "Should ask for SSN first (highest priority)");
        assertTrue(response1.getFailedTokens() == null || response1.getFailedTokens().isEmpty(), "No failed tokens initially");
        
        // STEP 2: User provides wrong SSN (first failure)
        context.setLastAskedToken("SSN");
        AuthenticationRequest request1 = new AuthenticationRequest(
                "demo-session-456",
                new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "555-1234"),
                "demo-attempt-123",
                List.of(new ProvidedToken("SSN", "wrong-ssn-1")),
                "DEMO_BANK"
        );
        
        when(tokenValidationService.validateToken(eq("SSN"), eq("DEMO_BANK"), 
                eq("555-1234"), eq("wrong-ssn-1"), eq(customerProfile))).thenReturn(false);
        
        tokenProcessingService.processProvidedTokens(request1, context, customerProfile);
        
        // Verify SSN has 1 attempt remaining and is marked for smart re-asking logic
        assertEquals(1, context.getTokenAttemptsRemaining().get("SSN"), "SSN should have 1 attempt remaining");
        assertTrue(context.hasAskedTokenValidationFailure("SSN"), "SSN should be marked as validation failure");
        assertFalse(context.canReAskToken("SSN"), "Should NOT be able to re-ask SSN (user provided it but failed)");
        assertFalse(context.isTokenFailed("SSN"), "SSN should NOT be in failed list yet (still has attempts)");
        
        // STEP 3: System should ask for next available token (DEBIT_CARD_PIN) since SSN can't be re-asked
        AuthenticationResponse response2 = authenticationResponseService.buildResponse(context, customerProfile, "DEMO_BANK");
        
        assertEquals("DEBIT_CARD_PIN", response2.getPrimaryTokenToAsk().getName(), 
                    "Should ask for DEBIT_CARD_PIN since SSN can't be re-asked");
        assertTrue(response2.getFailedTokens() == null || response2.getFailedTokens().isEmpty(), 
                  "No failed tokens yet (SSN still has attempts)");
        
        // STEP 4: User provides wrong SSN again (second failure - exhausts all attempts)
        context.setLastAskedToken("DEBIT_CARD_PIN"); // System asked for PIN
        AuthenticationRequest request2 = new AuthenticationRequest(
                "demo-session-456",
                new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "555-1234"),
                "demo-attempt-123",
                List.of(new ProvidedToken("SSN", "wrong-ssn-2")), // User provides SSN instead of PIN
                "DEMO_BANK"
        );
        
        when(tokenValidationService.validateToken(eq("SSN"), eq("DEMO_BANK"), 
                eq("555-1234"), eq("wrong-ssn-2"), eq(customerProfile))).thenReturn(false);
        
        tokenProcessingService.processProvidedTokens(request2, context, customerProfile);
        
        // Verify SSN is now completely failed
        assertEquals(0, context.getTokenAttemptsRemaining().get("SSN"), "SSN should have 0 attempts remaining");
        assertTrue(context.isTokenFailed("SSN"), "SSN should be in failed tokens list");
        assertFalse(context.canReAskToken("SSN"), "Should NOT be able to re-ask failed token");
        
        // STEP 5: System response should include SSN in failed tokens and exclude it from selection
        AuthenticationResponse response3 = authenticationResponseService.buildResponse(context, customerProfile, "DEMO_BANK");
        
        assertEquals("DEBIT_CARD_PIN", response3.getPrimaryTokenToAsk().getName(), 
                    "Should still ask for DEBIT_CARD_PIN");
        assertNotNull(response3.getFailedTokens(), "Failed tokens should not be null");
        assertTrue(response3.getFailedTokens().contains("SSN"), 
                  "Response should include SSN in failed tokens list");
        
        // Verify SSN is not in secondary tokens either
        boolean ssnInSecondary = response3.getSecondaryTokensAccepted().stream()
                .anyMatch(token -> "SSN".equals(token.getName()));
        assertFalse(ssnInSecondary, "SSN should NOT be available as secondary token");
        
        // STEP 6: User provides correct PIN
        context.setLastAskedToken("DEBIT_CARD_PIN");
        AuthenticationRequest request3 = new AuthenticationRequest(
                "demo-session-456",
                new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "555-1234"),
                "demo-attempt-123",
                List.of(new ProvidedToken("DEBIT_CARD_PIN", "1234")),
                "DEMO_BANK"
        );
        
        when(tokenValidationService.validateToken(eq("DEBIT_CARD_PIN"), eq("DEMO_BANK"), 
                eq("555-1234"), eq("1234"), eq(customerProfile))).thenReturn(true);
        
        tokenProcessingService.processProvidedTokens(request3, context, customerProfile);
        
        // Verify PIN is authenticated
        assertTrue(context.isTokenAuthenticated("DEBIT_CARD_PIN"), "PIN should be authenticated");
        
        // STEP 7: Since SSN is required but failed, and PIN alone isn't enough, 
        // system should ask for DATE_OF_BIRTH as alternative
        AuthenticationResponse response4 = authenticationResponseService.buildResponse(context, customerProfile, "DEMO_BANK");
        
        assertEquals("DATE_OF_BIRTH", response4.getPrimaryTokenToAsk().getName(), 
                    "Should ask for DATE_OF_BIRTH as alternative to failed SSN");
        assertTrue(response4.getFailedTokens().contains("SSN"), 
                  "Response should still include SSN in failed tokens");
        assertTrue(response4.getAuthenticatedTokens().contains("DEBIT_CARD_PIN"), 
                  "Response should include authenticated PIN");
        
        // STEP 8: Verify failed tokens are consistently included in responses
        // Even when we have some authenticated tokens, failed tokens should still be reported
        assertNotNull(response4.getFailedTokens(), "Failed tokens should be included in response");
        assertTrue(response4.getFailedTokens().contains("SSN"), 
                  "SSN should be in failed tokens list");
        
        System.out.println("=== FAILED TOKEN FLOW DEMONSTRATION COMPLETE ===");
        System.out.println("1. SSN failed validation twice and was added to failed tokens list");
        System.out.println("2. SSN was excluded from future token selection (primary and secondary)");
        System.out.println("3. Failed tokens were included in all authentication responses");
        System.out.println("4. Smart re-asking logic prevented re-asking SSN after user provided it");
        System.out.println("5. System successfully used alternative tokens for authentication");
        System.out.println("Failed tokens in response: " + response4.getFailedTokens());
        System.out.println("Authenticated tokens in response: " + response4.getAuthenticatedTokens());
    }
} 