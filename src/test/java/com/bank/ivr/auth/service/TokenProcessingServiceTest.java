package com.bank.ivr.auth.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenValidationResult;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.ProvidedToken;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;

@ExtendWith(MockitoExtension.class)
class TokenProcessingServiceTest {

    @Mock
    private TokenValidationService tokenValidationService;
    
    @Mock
    private BrandAuthConfigurationService brandConfigService;

    @InjectMocks
    private TokenProcessingService tokenProcessingService;

    private AuthenticationContext context;
    private CustomerProfile customerProfile;
    private AuthenticationRequest request;
    private List<AuthTokenDefinition> tokenDefinitions;

    @BeforeEach
    void setUp() {
        // Set up token definitions with inputFormatRegex
        tokenDefinitions = Arrays.asList(
            AuthTokenDefinition.builder()
                .name("SSN")
                .description("Social Security Number")
                .priority(100)
                .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                .maxAttempts(3)
                .build(),
            
            AuthTokenDefinition.builder()
                .name("DEBIT_CARD_PIN")
                .description("4-digit PIN")
                .priority(90)
                .inputFormatRegex("^\\d{4}$")
                .maxAttempts(3)
                .build(),
            
            AuthTokenDefinition.builder()
                .name("DATE_OF_BIRTH")
                .description("Date of Birth")
                .priority(80)
                .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                .maxAttempts(3)
                .build()
        );
        
        // Set up test data
        Map<String, Integer> tokenAttempts = new HashMap<>();
        tokenAttempts.put("SSN", 3);
        tokenAttempts.put("DEBIT_CARD_PIN", 3);
        tokenAttempts.put("DATE_OF_BIRTH", 3);

        context = AuthenticationContext.builder()
                .attemptId("test-attempt-123")
                .sessionId("test-session-456")
                .customerIdentifier(new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "1234567890"))
                .brand("TEST_BRAND")
                .startTime(LocalDateTime.now())
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(5)
                .eligibleTokens(Arrays.asList("SSN", "DEBIT_CARD_PIN", "DATE_OF_BIRTH"))
                .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                .build();

        customerProfile = new CustomerProfile();
        customerProfile.setSsn("123456789");
        customerProfile.setHashedPin("hashedPin123");
        customerProfile.setDateOfBirth(java.time.LocalDate.of(1985, 6, 15));

        // Base request setup - will be modified per test
        request = new AuthenticationRequest(
                "test-session-456",
                new CustomerIdentifier(CustomerIdentifier.IdentifierType.PHONE_NUMBER, "1234567890"),
                "test-attempt-123",
                null, // will be set per test
                "TEST_BRAND",
                createDefaultTrustLevelInfo()
        );
    }
    
    private TrustLevelInfo createDefaultTrustLevelInfo() {
        return new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
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
                Arrays.asList(pinToken),
                request.getBrand(),
                request.getTrustLevelInfo()
        );

        when(tokenValidationService.validateTokenWithPostValidation(eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("1234"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());

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
                Arrays.asList(ssnToken),
                request.getBrand(),
                request.getTrustLevelInfo()
        );

        when(tokenValidationService.validateTokenWithPostValidation(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("wrongssn"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.failure());

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
                Arrays.asList(ssnToken),
                request.getBrand(),
                request.getTrustLevelInfo()
        );

        when(tokenValidationService.validateTokenWithPostValidation(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("123456789"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());

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
                Arrays.asList(ssnToken),
                request.getBrand(),
                request.getTrustLevelInfo()
        );

        // Mock that validation succeeds (SSN validator should handle flexible input)
        when(tokenValidationService.validateTokenWithPostValidation(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("123456789"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());

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
                request.getBrand(),
                request.getTrustLevelInfo()
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
                Arrays.asList(ssnToken),
                request.getBrand(),
                request.getTrustLevelInfo()
        );

        when(tokenValidationService.validateTokenWithPostValidation(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("wrongssn"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.failure());

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
                Arrays.asList(ssnToken),
                request.getBrand(),
                request.getTrustLevelInfo()
        );

        when(tokenValidationService.validateTokenWithPostValidation(eq("SSN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("wrongssn"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.failure());

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertFalse(context.isTokenFailed("SSN"), "SSN should NOT be marked as failed when attempts remain");
        assertFalse(context.getFailedTokens().contains("SSN"), "SSN should NOT be in the failed tokens list");
        assertEquals(1, context.getTokenAttemptsRemaining().get("SSN"), "SSN should have 1 attempt remaining");
        assertFalse(context.canReAskToken("SSN"), "Should NOT be able to re-ask token that user provided but failed validation");
    }

    @Test
    void testSecondaryTokenDetection() {
        // Arrange: Set up secondary token detection scenario
        context.setLastAskedToken("SSN");
        context.addAskedToken("SSN");
        
        // User provides a secondary token
        ProvidedToken secondaryToken = new ProvidedToken("NEW_TOKEN", "secondaryTokenValue");
        request = new AuthenticationRequest(
                request.getSessionId(),
                request.getCustomerIdentifier(),
                request.getAttemptId(),
                Arrays.asList(secondaryToken),
                request.getBrand(),
                request.getTrustLevelInfo()
        );

        // Mock that validation succeeds (secondary token validator should handle secondary token)
        when(tokenValidationService.validateTokenWithPostValidation(eq("NEW_TOKEN"), eq("TEST_BRAND"), 
                eq("1234567890"), eq("secondaryTokenValue"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());

        // Act
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);

        // Assert
        assertTrue(context.isTokenAuthenticated("NEW_TOKEN"), "NEW_TOKEN should be authenticated");
        assertFalse(context.hasAskedTokenValidationFailure("NEW_TOKEN"), "NEW_TOKEN should not have validation failure");
    }

    @Nested
    @DisplayName("Secondary Token Detection Tests")
    class SecondaryTokenDetectionTests {
        
        @Test
        @DisplayName("Should detect SSN when user provides 9 digits as PIN")
        void shouldDetectSsnWhenUserProvides9DigitsAsPin() {
            // Given - User provides 9 digits as PIN, but it matches SSN pattern
            ProvidedToken pinToken = new ProvidedToken("DEBIT_CARD_PIN", "123456789");
            request = new AuthenticationRequest(
                    request.getSessionId(),
                    request.getCustomerIdentifier(),
                    request.getAttemptId(),
                    Arrays.asList(pinToken),
                    request.getBrand(),
                    request.getTrustLevelInfo()
            );
            
            // Mock brand config service
            when(brandConfigService.getTokenDefinitionsForBrand("TEST_BRAND")).thenReturn(tokenDefinitions);
            
            // Mock PIN fails but SSN succeeds
            when(tokenValidationService.validateTokenWithPostValidation(
                eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), eq("1234567890"), eq("123456789"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.failure());
            
            when(tokenValidationService.validateTokenWithPostValidation(
                eq("SSN"), eq("TEST_BRAND"), eq("1234567890"), eq("123456789"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());
            
            // When
            tokenProcessingService.processProvidedTokens(request, context, customerProfile);
            
            // Then - Both tokens should have been validated
            ArgumentCaptor<String> tokenNameCaptor = ArgumentCaptor.forClass(String.class);
            verify(tokenValidationService, times(2)).validateTokenWithPostValidation(
                tokenNameCaptor.capture(), eq("TEST_BRAND"), eq("1234567890"), eq("123456789"), 
                eq(customerProfile), eq(context));
            
            List<String> validatedTokens = tokenNameCaptor.getAllValues();
            assertTrue(validatedTokens.contains("DEBIT_CARD_PIN"), "Original PIN token should be validated");
            assertTrue(validatedTokens.contains("SSN"), "Secondary SSN token should be detected and validated");
            
            // SSN should be authenticated (successful validation)
            assertTrue(context.getAuthenticatedTokens().contains("SSN"), 
                     "SSN should be authenticated after successful validation");
        }
        
        @Test
        @DisplayName("Should detect date of birth from MM/dd/yyyy format")
        void shouldDetectDateOfBirthFromStandardFormat() {
            // Given - User provides date in MM/dd/yyyy format
            ProvidedToken dobToken = new ProvidedToken("DATE_OF_BIRTH", "06/15/1985");
            request = new AuthenticationRequest(
                    request.getSessionId(),
                    request.getCustomerIdentifier(),
                    request.getAttemptId(),
                    Arrays.asList(dobToken),
                    request.getBrand(),
                    request.getTrustLevelInfo()
            );
            
            when(brandConfigService.getTokenDefinitionsForBrand("TEST_BRAND")).thenReturn(tokenDefinitions);
            
            when(tokenValidationService.validateTokenWithPostValidation(
                eq("DATE_OF_BIRTH"), eq("TEST_BRAND"), eq("1234567890"), eq("06/15/1985"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());
            
            // When
            tokenProcessingService.processProvidedTokens(request, context, customerProfile);
            
            // Then
            verify(tokenValidationService, times(1)).validateTokenWithPostValidation(
                eq("DATE_OF_BIRTH"), eq("TEST_BRAND"), eq("1234567890"), eq("06/15/1985"), 
                eq(customerProfile), eq(context));
            
            assertTrue(context.getAuthenticatedTokens().contains("DATE_OF_BIRTH"), 
                     "DOB should be authenticated");
        }
        
        @Test
        @DisplayName("Should not detect secondary tokens when pattern doesn't match")
        void shouldNotDetectSecondaryTokensWhenPatternDoesntMatch() {
            // Given - User provides 3 digits as PIN (doesn't match any other patterns)
            ProvidedToken pinToken = new ProvidedToken("DEBIT_CARD_PIN", "123");
            request = new AuthenticationRequest(
                    request.getSessionId(),
                    request.getCustomerIdentifier(),
                    request.getAttemptId(),
                    Arrays.asList(pinToken),
                    request.getBrand(),
                    request.getTrustLevelInfo()
            );
            
            when(brandConfigService.getTokenDefinitionsForBrand("TEST_BRAND")).thenReturn(tokenDefinitions);
            
            when(tokenValidationService.validateTokenWithPostValidation(
                eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), eq("1234567890"), eq("123"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.failure());
            
            // When
            tokenProcessingService.processProvidedTokens(request, context, customerProfile);
            
            // Then - Only the original token should have been validated
            verify(tokenValidationService, times(1)).validateTokenWithPostValidation(
                eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), eq("1234567890"), eq("123"), 
                eq(customerProfile), eq(context));
        }
        
        @Test
        @DisplayName("Should handle invalid regex patterns gracefully")
        void shouldHandleInvalidRegexPatternsGracefully() {
            // Given - Token definition with invalid regex
            List<AuthTokenDefinition> invalidTokenDefs = Arrays.asList(
                AuthTokenDefinition.builder()
                    .name("INVALID_TOKEN")
                    .description("Invalid Token")
                    .priority(50)
                    .inputFormatRegex("[") // Invalid regex - missing closing bracket
                    .maxAttempts(3)
                    .build(),
                tokenDefinitions.get(1) // Valid PIN token
            );
            
            ProvidedToken pinToken = new ProvidedToken("DEBIT_CARD_PIN", "1234");
            request = new AuthenticationRequest(
                    request.getSessionId(),
                    request.getCustomerIdentifier(),
                    request.getAttemptId(),
                    Arrays.asList(pinToken),
                    request.getBrand(),
                    request.getTrustLevelInfo()
            );
            
            when(brandConfigService.getTokenDefinitionsForBrand("TEST_BRAND")).thenReturn(invalidTokenDefs);
            
            when(tokenValidationService.validateTokenWithPostValidation(
                eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), eq("1234567890"), eq("1234"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());
            
            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> {
                tokenProcessingService.processProvidedTokens(request, context, customerProfile);
            });
            
            // Should still validate the original token
            verify(tokenValidationService, times(1)).validateTokenWithPostValidation(
                eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), eq("1234567890"), eq("1234"), 
                eq(customerProfile), eq(context));
        }
        
        @Test
        @DisplayName("Should continue processing even when primary token fails")
        void shouldContinueProcessingEvenWhenPrimaryTokenFails() {
            // Given - Multiple tokens provided, first one fails
            ProvidedToken pinToken = new ProvidedToken("DEBIT_CARD_PIN", "9999"); // Will fail
            ProvidedToken ssnToken = new ProvidedToken("SSN", "123456789");        // Will succeed
            
            request = new AuthenticationRequest(
                    request.getSessionId(),
                    request.getCustomerIdentifier(),
                    request.getAttemptId(),
                    Arrays.asList(pinToken, ssnToken),
                    request.getBrand(),
                    request.getTrustLevelInfo()
            );
            
            when(brandConfigService.getTokenDefinitionsForBrand("TEST_BRAND")).thenReturn(tokenDefinitions);
            
            // Mock PIN failure, SSN success
            when(tokenValidationService.validateTokenWithPostValidation(
                eq("DEBIT_CARD_PIN"), eq("TEST_BRAND"), eq("1234567890"), eq("9999"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.failure());
            
            when(tokenValidationService.validateTokenWithPostValidation(
                eq("SSN"), eq("TEST_BRAND"), eq("1234567890"), eq("123456789"), eq(customerProfile), eq(context)))
                .thenReturn(TokenValidationResult.success());
            
            // When
            tokenProcessingService.processProvidedTokens(request, context, customerProfile);
            
            // Then - Both tokens should have been processed
            verify(tokenValidationService, times(2)).validateTokenWithPostValidation(
                any(), eq("TEST_BRAND"), eq("1234567890"), any(), eq(customerProfile), eq(context));
            
            // SSN should be authenticated despite PIN failure
            assertTrue(context.getAuthenticatedTokens().contains("SSN"), 
                     "SSN should be authenticated even though PIN failed");
            
            // Context should reflect that authentication partially succeeded
            assertFalse(context.getAuthenticatedTokens().isEmpty(), 
                      "At least one token should be authenticated");
        }
    }
} 