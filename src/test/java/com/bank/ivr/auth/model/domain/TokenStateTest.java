package com.bank.ivr.auth.model.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenState Tests")
class TokenStateTest {

    private TokenState tokenState;

    @BeforeEach
    void setUp() {
        List<String> eligibleTokens = Arrays.asList("SSN", "DEBIT_CARD_PIN", "DATE_OF_BIRTH");
        tokenState = TokenState.builder()
                .eligibleTokens(eligibleTokens)
                .authenticatedTokens(new ArrayList<>())
                .failedTokens(new ArrayList<>())
                .askedTokens(new ArrayList<>())
                .askedTokensWithValidationFailure(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("Should initialize with empty collections")
    void shouldInitializeWithEmptyCollections() {
        // Given - fresh TokenState
        
        // Then
        assertNotNull(tokenState.getEligibleTokens());
        assertEquals(3, tokenState.getEligibleTokens().size());
        assertTrue(tokenState.getAuthenticatedTokens().isEmpty());
        assertTrue(tokenState.getFailedTokens().isEmpty());
        assertTrue(tokenState.getAskedTokens().isEmpty());
        assertTrue(tokenState.getAskedTokensWithValidationFailure().isEmpty());
        assertNull(tokenState.getLastAskedToken());
    }

    @Test
    @DisplayName("Should add authenticated token without duplicates")
    void shouldAddAuthenticatedTokenWithoutDuplicates() {
        // When
        tokenState.addAuthenticatedToken("SSN");
        tokenState.addAuthenticatedToken("SSN"); // Duplicate
        tokenState.addAuthenticatedToken("DEBIT_CARD_PIN");

        // Then
        assertEquals(2, tokenState.getAuthenticatedTokens().size());
        assertTrue(tokenState.getAuthenticatedTokens().contains("SSN"));
        assertTrue(tokenState.getAuthenticatedTokens().contains("DEBIT_CARD_PIN"));
        assertTrue(tokenState.isTokenAuthenticated("SSN"));
        assertTrue(tokenState.isTokenAuthenticated("DEBIT_CARD_PIN"));
        assertFalse(tokenState.isTokenAuthenticated("DATE_OF_BIRTH"));
    }

    @Test
    @DisplayName("Should add failed token without duplicates")
    void shouldAddFailedTokenWithoutDuplicates() {
        // When
        tokenState.addFailedToken("SSN");
        tokenState.addFailedToken("SSN"); // Duplicate
        tokenState.addFailedToken("DATE_OF_BIRTH");

        // Then
        assertEquals(2, tokenState.getFailedTokens().size());
        assertTrue(tokenState.getFailedTokens().contains("SSN"));
        assertTrue(tokenState.getFailedTokens().contains("DATE_OF_BIRTH"));
        assertTrue(tokenState.isTokenFailed("SSN"));
        assertTrue(tokenState.isTokenFailed("DATE_OF_BIRTH"));
        assertFalse(tokenState.isTokenFailed("DEBIT_CARD_PIN"));
    }

    @Test
    @DisplayName("Should track asked tokens without duplicates")
    void shouldTrackAskedTokensWithoutDuplicates() {
        // When
        tokenState.addAskedToken("SSN");
        tokenState.addAskedToken("SSN"); // Duplicate
        tokenState.addAskedToken("DEBIT_CARD_PIN");

        // Then
        assertEquals(2, tokenState.getAskedTokens().size());
        assertTrue(tokenState.getAskedTokens().contains("SSN"));
        assertTrue(tokenState.getAskedTokens().contains("DEBIT_CARD_PIN"));
        assertTrue(tokenState.isTokenAlreadyAsked("SSN"));
        assertTrue(tokenState.isTokenAlreadyAsked("DEBIT_CARD_PIN"));
        assertFalse(tokenState.isTokenAlreadyAsked("DATE_OF_BIRTH"));
    }

    @Test
    @DisplayName("Should track validation failures and increment count")
    void shouldTrackValidationFailuresAndIncrementCount() {
        // When
        tokenState.markAskedTokenValidationFailure("SSN");
        tokenState.markAskedTokenValidationFailure("SSN"); // Second failure
        tokenState.markAskedTokenValidationFailure("DEBIT_CARD_PIN");

        // Then
        assertTrue(tokenState.hasAskedTokenValidationFailure("SSN"));
        assertTrue(tokenState.hasAskedTokenValidationFailure("DEBIT_CARD_PIN"));
        assertFalse(tokenState.hasAskedTokenValidationFailure("DATE_OF_BIRTH"));
        
        assertEquals(2, tokenState.getAskedTokenValidationFailureCount("SSN"));
        assertEquals(1, tokenState.getAskedTokenValidationFailureCount("DEBIT_CARD_PIN"));
        assertEquals(0, tokenState.getAskedTokenValidationFailureCount("DATE_OF_BIRTH"));
    }

    @Test
    @DisplayName("Smart re-asking logic: Should allow re-asking tokens that were asked but user didn't provide")
    void shouldAllowReAskingTokensUserDidntProvide() {
        // Given - Token was asked but user didn't provide it (no validation failure)
        tokenState.addAskedToken("SSN");

        // When/Then - Should allow re-asking since no validation failure
        assertTrue(tokenState.canReAskToken("SSN"));
    }

    @Test
    @DisplayName("Smart re-asking logic: Should NOT allow re-asking tokens that failed validation")
    void shouldNotAllowReAskingTokensThatFailedValidation() {
        // Given - Token was asked and user provided it but validation failed
        tokenState.addAskedToken("SSN");
        tokenState.markAskedTokenValidationFailure("SSN");

        // When/Then - Should NOT allow re-asking since validation failed
        assertFalse(tokenState.canReAskToken("SSN"));
    }

    @Test
    @DisplayName("Smart re-asking logic: Should allow asking tokens that were never asked")
    void shouldAllowAskingTokensNeverAsked() {
        // Given - Token was never asked
        
        // When/Then - Should allow asking
        assertTrue(tokenState.canReAskToken("SSN"));
        assertTrue(tokenState.canReAskToken("DEBIT_CARD_PIN"));
        assertTrue(tokenState.canReAskToken("DATE_OF_BIRTH"));
    }

    @Test
    @DisplayName("Should reset asked tokens for new attempt")
    void shouldResetAskedTokensForNewAttempt() {
        // Given - Some tokens were asked
        tokenState.addAskedToken("SSN");
        tokenState.addAskedToken("DEBIT_CARD_PIN");
        tokenState.setLastAskedToken("DEBIT_CARD_PIN");

        // When
        tokenState.resetAskedTokensForNewAttempt();

        // Then
        assertTrue(tokenState.getAskedTokens().isEmpty());
        // Note: lastAskedToken and validation failures are NOT reset
        assertEquals("DEBIT_CARD_PIN", tokenState.getLastAskedToken());
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
        // When/Then - Should not throw exceptions
        assertDoesNotThrow(() -> {
            tokenState.addAuthenticatedToken(null);
            tokenState.addFailedToken(null);
            tokenState.addAskedToken(null);
            tokenState.markAskedTokenValidationFailure(null);
            tokenState.isTokenAuthenticated(null);
            tokenState.isTokenFailed(null);
            tokenState.isTokenAlreadyAsked(null);
            tokenState.hasAskedTokenValidationFailure(null);
            tokenState.canReAskToken(null);
        });
    }

    @Test
    @DisplayName("Should build TokenState with builder pattern")
    void shouldBuildTokenStateWithBuilderPattern() {
        // Given
        List<String> eligibleTokens = Arrays.asList("TOKEN1", "TOKEN2");
        List<String> authenticatedTokens = Arrays.asList("TOKEN1");
        List<String> failedTokens = Arrays.asList("TOKEN3");
        List<String> askedTokens = Arrays.asList("TOKEN1", "TOKEN2");
        Map<String, Integer> validationFailures = new HashMap<>();
        validationFailures.put("TOKEN3", 2);

        // When
        TokenState builtTokenState = TokenState.builder()
                .eligibleTokens(eligibleTokens)
                .authenticatedTokens(authenticatedTokens)
                .failedTokens(failedTokens)
                .askedTokens(askedTokens)
                .lastAskedToken("TOKEN2")
                .askedTokensWithValidationFailure(validationFailures)
                .build();

        // Then
        assertEquals(eligibleTokens, builtTokenState.getEligibleTokens());
        assertEquals(authenticatedTokens, builtTokenState.getAuthenticatedTokens());
        assertEquals(failedTokens, builtTokenState.getFailedTokens());
        assertEquals(askedTokens, builtTokenState.getAskedTokens());
        assertEquals("TOKEN2", builtTokenState.getLastAskedToken());
        assertEquals(validationFailures, builtTokenState.getAskedTokensWithValidationFailure());
    }

    @Test
    @DisplayName("Should handle constructor with null collections")
    void shouldHandleConstructorWithNullCollections() {
        // When
        TokenState tokenStateWithNulls = new TokenState(
                null, null, null, null, null, null
        );

        // Then - Should initialize with empty collections
        assertNotNull(tokenStateWithNulls.getEligibleTokens());
        assertNotNull(tokenStateWithNulls.getAuthenticatedTokens());
        assertNotNull(tokenStateWithNulls.getFailedTokens());
        assertNotNull(tokenStateWithNulls.getAskedTokens());
        assertNotNull(tokenStateWithNulls.getAskedTokensWithValidationFailure());
        assertTrue(tokenStateWithNulls.getEligibleTokens().isEmpty());
        assertTrue(tokenStateWithNulls.getAuthenticatedTokens().isEmpty());
        assertTrue(tokenStateWithNulls.getFailedTokens().isEmpty());
        assertTrue(tokenStateWithNulls.getAskedTokens().isEmpty());
        assertTrue(tokenStateWithNulls.getAskedTokensWithValidationFailure().isEmpty());
    }

    @Test
    @DisplayName("Should demonstrate complete smart re-asking workflow")
    void shouldDemonstrateCompleteSmartReAskingWorkflow() {
        // Scenario: User authentication flow with smart re-asking logic
        
        // Step 1: System asks for SSN
        tokenState.addAskedToken("SSN");
        tokenState.setLastAskedToken("SSN");
        assertTrue(tokenState.canReAskToken("SSN")); // Can re-ask since no validation failure yet
        
        // Step 2: User provides wrong SSN - validation fails
        tokenState.markAskedTokenValidationFailure("SSN");
        assertFalse(tokenState.canReAskToken("SSN")); // Cannot re-ask anymore
        
        // Step 3: System asks for PIN (next priority)
        tokenState.addAskedToken("DEBIT_CARD_PIN");
        tokenState.setLastAskedToken("DEBIT_CARD_PIN");
        assertTrue(tokenState.canReAskToken("DEBIT_CARD_PIN")); // Can ask since never failed validation
        
        // Step 4: User provides correct PIN
        tokenState.addAuthenticatedToken("DEBIT_CARD_PIN");
        assertTrue(tokenState.isTokenAuthenticated("DEBIT_CARD_PIN"));
        
        // Step 5: System asks for DOB
        tokenState.addAskedToken("DATE_OF_BIRTH");
        tokenState.setLastAskedToken("DATE_OF_BIRTH");
        assertTrue(tokenState.canReAskToken("DATE_OF_BIRTH"));
        
        // Step 6: User provides correct DOB
        tokenState.addAuthenticatedToken("DATE_OF_BIRTH");
        
        // Final state verification
        assertEquals(2, tokenState.getAuthenticatedTokens().size());
        assertEquals(0, tokenState.getFailedTokens().size());
        assertEquals(3, tokenState.getAskedTokens().size());
        assertEquals(1, tokenState.getAskedTokensWithValidationFailure().size());
        
        // Smart re-asking verification
        assertFalse(tokenState.canReAskToken("SSN")); // Failed validation
        assertTrue(tokenState.canReAskToken("DEBIT_CARD_PIN")); // Authenticated, but could theoretically be re-asked
        assertTrue(tokenState.canReAskToken("DATE_OF_BIRTH")); // Authenticated, but could theoretically be re-asked
    }
} 