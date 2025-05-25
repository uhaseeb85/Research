package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.ProvidedToken;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class TokenProcessingServiceTest {

    @Mock
    private TokenValidationService tokenValidationService;

    @InjectMocks
    private TokenProcessingService tokenProcessingService;

    private AuthenticationContext context;
    private CustomerProfile customerProfile;
    private AuthenticationRequest request;

    @BeforeEach
    void setUp() {
        // Set up test data
        Map<String, Integer> tokenAttempts = new HashMap<>();
        tokenAttempts.put("SSN", 3);
        tokenAttempts.put("DEBIT_CARD_PIN", 3);

        context = AuthenticationContext.builder()
                .attemptId("test-attempt-123")
                .sessionId("test-session-456")
                .customerIdentifier(new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "1234567890"))
                .brand("TEST_BRAND")
                .startTime(LocalDateTime.now())
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(5)
                .eligibleTokens(Arrays.asList("SSN", "DEBIT_CARD_PIN"))
                .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                .build();

        customerProfile = new CustomerProfile();
        customerProfile.setSsn("123456789");
        customerProfile.setHashedPin("hashedPin123");

        // Base request setup - will be modified per test
        request = new AuthenticationRequest(
                "test-session-456",
                new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "1234567890"),
                "test-attempt-123",
                null, // will be set per test
                "TEST_BRAND"
        );
    }

    @Test
    void testSmartReAskingLogic_UserDidNotProvideAskedToken() {
        // Arrange: We asked for SSN but user provided PIN instead
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        ProvidedToken pinToken = new ProvidedToken("DEBIT_CARD_PIN", "1234");
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                List.of(pinToken),
                request.getBrand()
        );

        when(tokenValidationService.validateToken(eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("1234"), eq(customerProfile))).thenReturn(true);

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertTrue(context.canReAskToken("SSN"), "Should be able to re-ask SSN since user didn't provide it");
        assertFalse(context.hasAskedTokenValidationFailure("SSN"), "SSN should not have validation failure");
        assertTrue(context.isTokenAuthenticated("DEBIT_CARD_PIN"), "PIN should be authenticated");
    }

    @Test
    void testSmartReAskingLogic_UserProvidedAskedTokenButValidationFailed() {
        // Arrange: We asked for SSN and user provided SSN but validation failed
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        ProvidedToken ssnToken = new ProvidedToken("SSN", "wrongssn");
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                List.of(ssnToken),
                request.getBrand()
        );

        when(tokenValidationService.validateToken(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("wrongssn"), eq(customerProfile))).thenReturn(false);

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertFalse(context.canReAskToken("SSN"), "Should NOT be able to re-ask SSN since user provided it but validation failed");
        assertTrue(context.hasAskedTokenValidationFailure("SSN"), "SSN should have validation failure marked");
        assertEquals(1, context.getAskedTokenValidationFailureCount("SSN"), "Should have 1 validation failure");
        assertFalse(context.isTokenAuthenticated("SSN"), "SSN should not be authenticated");
    }

    @Test
    void testSmartReAskingLogic_UserProvidedAskedTokenAndValidationSucceeded() {
        // Arrange: We asked for SSN and user provided SSN and validation succeeded
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        ProvidedToken ssnToken = new ProvidedToken("SSN", "123456789");
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                List.of(ssnToken),
                request.getBrand()
        );

        when(tokenValidationService.validateToken(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("123456789"), eq(customerProfile))).thenReturn(true);

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertTrue(context.isTokenAuthenticated("SSN"), "SSN should be authenticated");
        assertFalse(context.hasAskedTokenValidationFailure("SSN"), "SSN should not have validation failure");
    }

    @Test
    void testSmartReAskingLogic_FlexibleTokenHandling() {
        // Arrange: We asked for SSN (last 4 digits) but user provided full SSN
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        // User provides full SSN when we asked for last 4 digits
        ProvidedToken ssnToken = new ProvidedToken("SSN", "123456789");
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                List.of(ssnToken),
                request.getBrand()
        );

        // Mock that validation succeeds (SSN validator should handle flexible input)
        when(tokenValidationService.validateToken(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("123456789"), eq(customerProfile))).thenReturn(true);

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertTrue(context.isTokenAuthenticated("SSN"), "SSN should be authenticated even with full SSN when asked for last 4");
        assertFalse(context.hasAskedTokenValidationFailure("SSN"), "SSN should not have validation failure");
    }

    @Test
    void testSmartReAskingLogic_NoTokensProvided() {
        // Arrange: We asked for SSN but user provided no tokens
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                null,
                request.getBrand()
        );

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertTrue(context.canReAskToken("SSN"), "Should be able to re-ask SSN since user didn't provide any token");
        assertFalse(context.hasAskedTokenValidationFailure("SSN"), "SSN should not have validation failure");
        assertEquals(2, context.getTokenAttemptsRemaining().get("SSN"), "SSN attempts should be decremented");
        assertEquals(4, context.getOverallAttemptsRemaining(), "Overall attempts should be decremented");
    }

    @Test
    void testContextResetAskedTokensForNewAttempt() {
        // Arrange: Add some asked tokens and validation failures
        context.addAskedToken("SSN");
        context.addAskedToken("DEBIT_CARD_PIN");
        context.markAskedTokenValidationFailure("SSN");

        // Act
        context.resetAskedTokensForNewAttempt();

        // Assert
        assertTrue(context.getAskedTokens().isEmpty(), "Asked tokens should be reset");
        assertTrue(context.hasAskedTokenValidationFailure("SSN"), "Validation failures should persist across attempts");
    }

    @Test
    void testCanReAskTokenLogic() {
        // Test new token (never asked)
        assertTrue(context.canReAskToken("NEW_TOKEN"), "Should be able to ask new token");

        // Test asked token without validation failure
        context.addAskedToken("SSN");
        assertTrue(context.canReAskToken("SSN"), "Should be able to re-ask token if user didn't provide it");

        // Test asked token with validation failure
        context.markAskedTokenValidationFailure("SSN");
        assertFalse(context.canReAskToken("SSN"), "Should NOT be able to re-ask token if user provided it but validation failed");
    }

    @Test
    void testTokenAddedToFailedListWhenAttemptsExhausted() {
        // Arrange: Set SSN to have only 1 attempt remaining
        Map<String, Integer> tokenAttempts = new HashMap<>();
        tokenAttempts.put("SSN", 1);
        tokenAttempts.put("DEBIT_CARD_PIN", 3);
        context.setTokenAttemptsRemaining(tokenAttempts);
        
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        ProvidedToken ssnToken = new ProvidedToken("SSN", "wrongssn");
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                List.of(ssnToken),
                request.getBrand()
        );

        when(tokenValidationService.validateToken(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("wrongssn"), eq(customerProfile))).thenReturn(false);

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertTrue(context.isTokenFailed("SSN"), "SSN should be marked as failed when attempts are exhausted");
        assertTrue(context.getFailedTokens().contains("SSN"), "SSN should be in the failed tokens list");
        assertEquals(0, context.getTokenAttemptsRemaining().get("SSN"), "SSN should have 0 attempts remaining");
        assertFalse(context.canReAskToken("SSN"), "Should NOT be able to re-ask failed token");
    }

    @Test
    void testTokenNotAddedToFailedListWhenAttemptsRemain() {
        // Arrange: Set SSN to have 2 attempts remaining
        Map<String, Integer> tokenAttempts = new HashMap<>();
        tokenAttempts.put("SSN", 2);
        tokenAttempts.put("DEBIT_CARD_PIN", 3);
        context.setTokenAttemptsRemaining(tokenAttempts);
        
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        ProvidedToken ssnToken = new ProvidedToken("SSN", "wrongssn");
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                List.of(ssnToken),
                request.getBrand()
        );

        when(tokenValidationService.validateToken(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("wrongssn"), eq(customerProfile))).thenReturn(false);

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertFalse(context.isTokenFailed("SSN"), "SSN should NOT be marked as failed when attempts remain");
        assertFalse(context.getFailedTokens().contains("SSN"), "SSN should NOT be in the failed tokens list");
        assertEquals(1, context.getTokenAttemptsRemaining().get("SSN"), "SSN should have 1 attempt remaining");
        assertFalse(context.canReAskToken("SSN"), "Should NOT be able to re-ask token that user provided but failed validation");
    }
} 