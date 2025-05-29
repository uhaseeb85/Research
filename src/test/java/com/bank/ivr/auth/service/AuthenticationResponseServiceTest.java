package com.bank.ivr.auth.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationResponseService Tests")
class AuthenticationResponseServiceTest {

    @Mock
    private AuthenticationContextService contextService;

    @Mock
    private BrandAuthConfigurationService brandConfigService;

    @InjectMocks
    private AuthenticationResponseService authenticationResponseService;

    private AuthenticationContext context;
    private CustomerProfile customerProfile;
    private List<AuthTokenDefinition> tokenDefinitions;

    @BeforeEach
    void setUp() {
        // Set up token definitions
        tokenDefinitions = Arrays.asList(
            AuthTokenDefinition.builder()
                    .name("SSN")
                    .description("Social Security Number")
                    .priority(100)
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("DEBIT_CARD_PIN")
                    .description("4-digit PIN")
                    .priority(90)
                    .maxAttempts(3)
                    .build(),
            
            AuthTokenDefinition.builder()
                    .name("DATE_OF_BIRTH")
                    .description("Date of Birth")
                    .priority(80)
                    .maxAttempts(3)
                    .build()
        );

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
                .brand("TEST_BANK")
                .startTime(LocalDateTime.now())
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(5)
                .eligibleTokens(Arrays.asList("SSN", "DEBIT_CARD_PIN", "DATE_OF_BIRTH"))
                .authenticatedTokens(new ArrayList<>())
                .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                .failedTokens(new ArrayList<>())
                .askedTokens(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should ask highest priority token first when no tokens have been asked")
    void shouldAskHighestPriorityTokenFirst() {
        // Given
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK")).thenReturn(tokenDefinitions);

        // When
        AuthenticationResponse response = authenticationResponseService.buildResponse(context, customerProfile, "TEST_BANK");

        // Then
        assertNotNull(response);
        assertNotNull(response.getPrimaryTokenToAsk());
        assertEquals("SSN", response.getPrimaryTokenToAsk().getName(), "Should ask SSN first as it has highest priority (100)");
        assertTrue(context.getAskedTokens().contains("SSN"), "SSN should be tracked as asked");
        assertEquals(1, context.getAskedTokens().size(), "Only one token should be tracked as asked");
    }

    @Test
    @DisplayName("Should allow re-asking token that was asked but user didn't provide")
    void shouldNotAskAlreadyAskedToken() {
        // Given - SSN was already asked but user didn't provide it (no validation failure)
        context.addAskedToken("SSN");
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK")).thenReturn(tokenDefinitions);

        // When
        AuthenticationResponse response = authenticationResponseService.buildResponse(context, customerProfile, "TEST_BANK");

        // Then
        assertNotNull(response);
        assertNotNull(response.getPrimaryTokenToAsk());
        // With smart re-asking logic, SSN can be re-asked since user didn't provide it
        assertEquals("SSN", response.getPrimaryTokenToAsk().getName(), 
                    "Should re-ask SSN since user didn't provide it when asked");
        assertTrue(context.getAskedTokens().contains("SSN"), "SSN should still be tracked as previously asked");
        assertEquals(1, context.getAskedTokens().size(), "SSN should be the only token tracked as asked");
    }

    @Test
    @DisplayName("Should not re-ask token that user provided but failed validation")
    void shouldSkipMultipleAlreadyAskedTokens() {
        // Given - SSN was asked and user provided it but validation failed
        context.addAskedToken("SSN");
        context.markAskedTokenValidationFailure("SSN"); // User provided SSN but it failed validation
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK")).thenReturn(tokenDefinitions);

        // When
        AuthenticationResponse response = authenticationResponseService.buildResponse(context, customerProfile, "TEST_BANK");

        // Then
        assertNotNull(response);
        assertNotNull(response.getPrimaryTokenToAsk());
        assertEquals("DEBIT_CARD_PIN", response.getPrimaryTokenToAsk().getName(), 
                    "Should ask DEBIT_CARD_PIN since SSN failed validation after user provided it");
        assertTrue(context.getAskedTokens().contains("SSN"), "SSN should still be tracked as previously asked");
        assertTrue(context.getAskedTokens().contains("DEBIT_CARD_PIN"), "DEBIT_CARD_PIN should now be tracked as asked");
        assertEquals(2, context.getAskedTokens().size(), "Both SSN and DEBIT_CARD_PIN should be tracked as asked");
    }

    @Test
    @DisplayName("Should fail when no tokens available after validation failures")
    void shouldFailWhenNoTokensAvailable() {
        // Given - all eligible tokens were asked and user provided them but they failed validation
        context.addAskedToken("SSN");
        context.addAskedToken("DEBIT_CARD_PIN");
        context.addAskedToken("DATE_OF_BIRTH");
        context.markAskedTokenValidationFailure("SSN");
        context.markAskedTokenValidationFailure("DEBIT_CARD_PIN");
        context.markAskedTokenValidationFailure("DATE_OF_BIRTH");
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK")).thenReturn(tokenDefinitions);
        when(brandConfigService.getBrandMessage("TEST_BANK", "no_methods")).thenReturn("No available authentication methods.");

        // When
        AuthenticationResponse response = authenticationResponseService.buildResponse(context, customerProfile, "TEST_BANK");

        // Then
        assertNotNull(response);
        assertEquals(AuthStatus.FAILED, response.getStatus(), "Should fail when no more tokens available");
        assertNull(response.getPrimaryTokenToAsk(), "Should not have a primary token to ask");
        assertEquals("No available authentication methods.", response.getMessage());
        verify(contextService).deleteContext("test-attempt-123");
    }

    @Test
    @DisplayName("Should still allow secondary tokens that were previously asked")
    void shouldAllowSecondaryTokensThatWerePreviouslyAsked() {
        // Given - SSN was asked and user provided it but validation failed
        context.addAskedToken("SSN");
        context.markAskedTokenValidationFailure("SSN"); // User provided SSN but it failed validation
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK")).thenReturn(tokenDefinitions);

        // When
        AuthenticationResponse response = authenticationResponseService.buildResponse(context, customerProfile, "TEST_BANK");

        // Then
        assertNotNull(response);
        assertEquals("DEBIT_CARD_PIN", response.getPrimaryTokenToAsk().getName(), "Primary should be DEBIT_CARD_PIN");
        
        List<AuthTokenDefinition> secondaryTokens = response.getSecondaryTokensAccepted();
        assertNotNull(secondaryTokens);
        
        // Check if DATE_OF_BIRTH is available as a secondary token
        boolean dobInSecondary = secondaryTokens.stream()
                .anyMatch(token -> "DATE_OF_BIRTH".equals(token.getName()));
        assertTrue(dobInSecondary, "DATE_OF_BIRTH should be available as a secondary token");
        
        // SSN should NOT be in secondary tokens since user provided it but validation failed
        boolean ssnInSecondary = secondaryTokens.stream()
                .anyMatch(token -> "SSN".equals(token.getName()));
        assertFalse(ssnInSecondary, "SSN should NOT be available as secondary token since user provided it but validation failed");
    }

    @Test
    @DisplayName("Should include failed tokens in response")
    void shouldIncludeFailedTokensInResponse() {
        // Given - SSN has failed (exhausted all attempts)
        context.addFailedToken("SSN");
        context.getTokenAttemptsRemaining().put("SSN", 0); // No attempts remaining
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK")).thenReturn(tokenDefinitions);

        // When
        AuthenticationResponse response = authenticationResponseService.buildResponse(context, customerProfile, "TEST_BANK");

        // Then
        assertNotNull(response);
        assertNotNull(response.getFailedTokens());
        assertTrue(response.getFailedTokens().contains("SSN"), "Response should include SSN in failed tokens list");
        assertEquals("DEBIT_CARD_PIN", response.getPrimaryTokenToAsk().getName(), 
                    "Should ask DEBIT_CARD_PIN since SSN has failed");
    }

    @Test
    @DisplayName("Should include failed tokens in successful authentication response")
    void shouldIncludeFailedTokensInSuccessfulResponse() {
        // Given - Authentication is complete but some tokens failed during the process
        context.addAuthenticatedToken("SSN");
        context.addAuthenticatedToken("DEBIT_CARD_PIN");
        context.addFailedToken("DATE_OF_BIRTH"); // This token failed during the process
        when(brandConfigService.getTokenDefinitionsForBrand("TEST_BANK")).thenReturn(tokenDefinitions);

        // When
        AuthenticationResponse response = authenticationResponseService.buildResponse(context, customerProfile, "TEST_BANK");

        // Then
        assertNotNull(response);
        assertEquals(AuthStatus.AUTHENTICATED, response.getStatus(), "Should be authenticated");
        assertNotNull(response.getFailedTokens());
        assertTrue(response.getFailedTokens().contains("DATE_OF_BIRTH"), 
                  "Response should include DATE_OF_BIRTH in failed tokens list even on success");
        assertNotNull(response.getAuthenticatedTokens());
        assertTrue(response.getAuthenticatedTokens().contains("SSN"));
        assertTrue(response.getAuthenticatedTokens().contains("DEBIT_CARD_PIN"));
    }
} 