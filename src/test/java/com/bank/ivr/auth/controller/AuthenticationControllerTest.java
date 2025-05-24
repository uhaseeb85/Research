package com.bank.ivr.auth.controller;

import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.ProvidedToken;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.service.AuthenticationOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@DisplayName("Authentication Controller Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockBean    private AuthenticationOrchestrator authenticationOrchestrator;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationRequest baseRequest;
    private CustomerIdentifier customerIdentifier;

    @BeforeEach
    void setUp() {
        customerIdentifier = new CustomerIdentifier(
                CustomerIdentifier.IdentifierType.PHONE_NUMBER,
                "+1234567890"
        );
        
        baseRequest = new AuthenticationRequest(
                "session-123",
                customerIdentifier,
                null, // New attempt
                Collections.emptyList()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - New Authentication Attempts")
    class NewAuthenticationAttempts {

        @Test
        @DisplayName("Should initiate new authentication successfully for valid customer")
        void shouldInitiateNewAuthenticationSuccessfully() throws Exception {
            // Given
            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your Social Security Number")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(baseRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId", is("attempt-123")))
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")))
                    .andExpect(jsonPath("$.message", containsString("Social Security Number")));

            verify(authenticationOrchestrator, times(1)).authenticateCustomer(any(AuthenticationRequest.class));
        }

        @Test
        @DisplayName("Should handle customer not found scenario")
        void shouldHandleCustomerNotFound() throws Exception {
            // Given
            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .status(AuthStatus.FAILED)
                    .message("Customer not found. Please verify your information.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(baseRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("Customer not found")));
        }

        @Test
        @DisplayName("Should validate required fields - missing sessionId")
        void shouldValidateMissingSessionId() throws Exception {
            // Given
            AuthenticationRequest invalidRequest = new AuthenticationRequest(
                    null, // Invalid - null sessionId
                    customerIdentifier,
                    null,
                    Collections.emptyList()
            );

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authenticationOrchestrator, never()).authenticateCustomer(any());
        }

        @Test
        @DisplayName("Should validate required fields - missing customer identifier")
        void shouldValidateMissingCustomerIdentifier() throws Exception {
            // Given
            AuthenticationRequest invalidRequest = new AuthenticationRequest(
                    "session-123",
                    null, // Invalid - null customer identifier
                    null,
                    Collections.emptyList()
            );

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authenticationOrchestrator, never()).authenticateCustomer(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Continuing Authentication Attempts")
    class ContinuingAuthenticationAttempts {

        @Test
        @DisplayName("Should handle valid token submission in continuing attempt")
        void shouldHandleValidTokenSubmission() throws Exception {
            // Given
            List<ProvidedToken> tokens = Arrays.asList(
                    new ProvidedToken("SSN", "123456789")
            );
            
            AuthenticationRequest continuingRequest = new AuthenticationRequest(
                    "session-123",
                    customerIdentifier,
                    "attempt-123", // Continuing attempt
                    tokens
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_MORE_TOKENS)
                    .message("Please provide your Date of Birth")
                    .authenticatedTokens(Arrays.asList("SSN"))
                    .requiredTokensRemaining(Arrays.asList("DOB"))
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(continuingRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId", is("attempt-123")))
                    .andExpect(jsonPath("$.status", is("PENDING_MORE_TOKENS")))
                    .andExpect(jsonPath("$.authenticatedTokens[0]", is("SSN")))
                    .andExpect(jsonPath("$.requiredTokensRemaining[0]", is("DOB")));
        }

        @Test
        @DisplayName("Should handle successful final authentication")
        void shouldHandleSuccessfulFinalAuthentication() throws Exception {
            // Given
            List<ProvidedToken> tokens = Arrays.asList(
                    new ProvidedToken("DOB", "1990-01-01")
            );
            
            AuthenticationRequest finalRequest = new AuthenticationRequest(
                    "session-123",
                    customerIdentifier,
                    "attempt-123",
                    tokens
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.AUTHENTICATED)
                    .message("Authentication successful")
                    .authenticatedTokens(Arrays.asList("SSN", "DOB"))
                    .requiredTokensRemaining(Collections.emptyList())
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(finalRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId", is("attempt-123")))
                    .andExpect(jsonPath("$.status", is("AUTHENTICATED")))
                    .andExpect(jsonPath("$.message", is("Authentication successful")))
                    .andExpect(jsonPath("$.authenticatedTokens.length()", is(2)))
                    .andExpect(jsonPath("$.requiredTokensRemaining.length()", is(0)));
        }

        @Test
        @DisplayName("Should handle authentication failure - invalid token")
        void shouldHandleAuthenticationFailureInvalidToken() throws Exception {
            // Given
            List<ProvidedToken> tokens = Arrays.asList(
                    new ProvidedToken("SSN", "invalid-ssn")
            );
            
            AuthenticationRequest failingRequest = new AuthenticationRequest(
                    "session-123",
                    customerIdentifier,
                    "attempt-123",
                    tokens
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.FAILED)
                    .message("Authentication failed. Invalid credentials provided.")
                    .remainingAttempts(Map.of("SSN", 2, "overall", 4))
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(failingRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId", is("attempt-123")))
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("Authentication failed")))
                    .andExpect(jsonPath("$.remainingAttempts.SSN", is(2)))
                    .andExpect(jsonPath("$.remainingAttempts.overall", is(4)));
        }

        @Test
        @DisplayName("Should handle expired session")
        void shouldHandleExpiredSession() throws Exception {
            // Given
            AuthenticationRequest expiredRequest = new AuthenticationRequest(
                    "session-123",
                    customerIdentifier,
                    "expired-attempt-123",
                    Collections.emptyList()
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("expired-attempt-123")
                    .status(AuthStatus.FAILED)
                    .message("Authentication session expired. Please start over.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(expiredRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("session expired")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Multiple Authentication Flow Scenarios")
    class MultipleAuthenticationFlowScenarios {

        @Test
        @DisplayName("Should handle complete successful authentication flow")
        void shouldHandleCompleteSuccessfulFlow() throws Exception {
            // Given - Mock responses for each step
            AuthenticationResponse step1Response = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your Social Security Number")
                    .build();

            AuthenticationResponse step2Response = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_MORE_TOKENS)
                    .message("Please provide your Date of Birth")
                    .authenticatedTokens(Arrays.asList("SSN"))
                    .requiredTokensRemaining(Arrays.asList("DOB"))
                    .build();

            AuthenticationResponse step3Response = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.AUTHENTICATED)
                    .message("Authentication successful")
                    .authenticatedTokens(Arrays.asList("SSN", "DOB"))
                    .requiredTokensRemaining(Collections.emptyList())
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(step1Response)
                    .thenReturn(step2Response)
                    .thenReturn(step3Response);

            // Step 1: Initial authentication request
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(baseRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));

            // Step 2: Provide SSN
            List<ProvidedToken> ssnTokens = Arrays.asList(new ProvidedToken("SSN", "123456789"));
            AuthenticationRequest step2Request = new AuthenticationRequest(
                    "session-123", customerIdentifier, "attempt-123", ssnTokens);

            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(step2Request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_MORE_TOKENS")))
                    .andExpect(jsonPath("$.authenticatedTokens[0]", is("SSN")));

            // Step 3: Provide DOB and complete authentication
            List<ProvidedToken> dobTokens = Arrays.asList(new ProvidedToken("DOB", "1990-01-01"));
            AuthenticationRequest step3Request = new AuthenticationRequest(
                    "session-123", customerIdentifier, "attempt-123", dobTokens);

            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(step3Request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("AUTHENTICATED")))
                    .andExpect(jsonPath("$.authenticatedTokens.length()", is(2)));

            // Verify all service calls were made
            verify(authenticationOrchestrator, times(3)).authenticateCustomer(any(AuthenticationRequest.class));
        }

        @Test
        @DisplayName("Should handle authentication failure after multiple attempts")
        void shouldHandleAuthenticationFailureAfterMultipleAttempts() throws Exception {
            // Given - Mock responses for each step
            AuthenticationResponse step1Response = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your Social Security Number")
                    .build();

            AuthenticationResponse step2Response = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Invalid SSN. Please try again. 2 attempts remaining.")
                    .remainingAttempts(Map.of("SSN", 2, "overall", 4))
                    .build();

            AuthenticationResponse step3Response = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.FAILED)
                    .message("Authentication failed. Maximum attempts exceeded.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(step1Response)
                    .thenReturn(step2Response)
                    .thenReturn(step3Response);

            // Step 1: Initial request
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(baseRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));

            // Step 2: First failed SSN attempt
            List<ProvidedToken> wrongSsnTokens = Arrays.asList(new ProvidedToken("SSN", "wrong-ssn"));
            AuthenticationRequest step2Request = new AuthenticationRequest(
                    "session-123", customerIdentifier, "attempt-123", wrongSsnTokens);

            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(step2Request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")))
                    .andExpect(jsonPath("$.remainingAttempts.SSN", is(2)));

            // Step 3: Final failed attempt leading to failure
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(step2Request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("Maximum attempts exceeded")));

            verify(authenticationOrchestrator, times(3)).authenticateCustomer(any(AuthenticationRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle IllegalArgumentException from service")
        void shouldHandleIllegalArgumentException() throws Exception {
            // Given
            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenThrow(new IllegalArgumentException("Invalid customer identifier format"));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(baseRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("Invalid request")));
        }

        @Test
        @DisplayName("Should handle unexpected exceptions from service")
        void shouldHandleUnexpectedExceptions() throws Exception {
            // Given
            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(baseRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("unexpected error occurred")));
        }

        @Test
        @DisplayName("Should handle malformed JSON request")
        void shouldHandleMalformedJsonRequest() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid-json}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authenticationOrchestrator, never()).authenticateCustomer(any());
        }

        @Test
        @DisplayName("Should handle missing Content-Type header")
        void shouldHandleMissingContentType() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .content(objectMapper.writeValueAsString(baseRequest)))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType());

            verify(authenticationOrchestrator, never()).authenticateCustomer(any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/health - Health Check")
    class HealthCheck {

        @Test
        @DisplayName("Should return healthy status")
        void shouldReturnHealthyStatus() throws Exception {
            mockMvc.perform(get("/api/v1/auth/health"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("IVR Authentication Service is healthy"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/methods - Authentication Methods")
    class AuthenticationMethods {

        @Test
        @DisplayName("Should return authentication methods info")
        void shouldReturnAuthenticationMethodsInfo() throws Exception {
            mockMvc.perform(get("/api/v1/auth/methods"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Authentication methods endpoint")));
        }
    }

    @Nested
    @DisplayName("Cross-Origin Resource Sharing (CORS)")
    class CorsTests {

        @Test
        @DisplayName("Should handle CORS preflight request")
        void shouldHandleCorsPreflight() throws Exception {
            mockMvc.perform(options("/api/v1/auth/customer")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Token Validation Edge Cases")
    class TokenValidationEdgeCases {

        @Test
        @DisplayName("Should handle empty token list")
        void shouldHandleEmptyTokenList() throws Exception {
            // Given
            AuthenticationRequest requestWithEmptyTokens = new AuthenticationRequest(
                    "session-123",
                    customerIdentifier,
                    "attempt-123",
                    Collections.emptyList()
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your Social Security Number")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithEmptyTokens)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));
        }

        @Test
        @DisplayName("Should handle null token list")
        void shouldHandleNullTokenList() throws Exception {
            // Given
            AuthenticationRequest requestWithNullTokens = new AuthenticationRequest(
                    "session-123",
                    customerIdentifier,
                    "attempt-123",
                    null
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your Social Security Number")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithNullTokens)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));
        }

        @Test
        @DisplayName("Should validate ProvidedToken fields")
        void shouldValidateProvidedTokenFields() throws Exception {
            // Given - ProvidedToken with empty token name
            String invalidJson = """
                    {
                        "sessionId": "session-123",
                        "customerIdentifier": {
                            "type": "PHONE_NUMBER",
                            "value": "+1234567890"
                        },
                        "attemptId": "attempt-123",
                        "providedTokens": [
                            {
                                "tokenName": "",
                                "tokenValue": "some-value"
                            }
                        ]
                    }
                    """;

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authenticationOrchestrator, never()).authenticateCustomer(any());
        }
    }

    @Nested
    @DisplayName("Different Customer Identifier Types")
    class CustomerIdentifierTypes {

        @Test
        @DisplayName("Should handle ACCOUNT_NUMBER identifier type")
        void shouldHandleAccountNumberIdentifier() throws Exception {
            // Given
            CustomerIdentifier accountIdentifier = new CustomerIdentifier(
                    CustomerIdentifier.IdentifierType.ACCOUNT_NUMBER,
                    "ACC123456789"
            );
            
            AuthenticationRequest accountRequest = new AuthenticationRequest(
                    "session-456",
                    accountIdentifier,
                    null,
                    Collections.emptyList()
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-456")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your Social Security Number")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));
        }

        @Test
        @DisplayName("Should handle CUSTOMER_ID identifier type")
        void shouldHandleCustomerIdIdentifier() throws Exception {
            // Given
            CustomerIdentifier customerIdIdentifier = new CustomerIdentifier(
                    CustomerIdentifier.IdentifierType.CUSTOMER_ID,
                    "CUST789123"
            );
            
            AuthenticationRequest customerIdRequest = new AuthenticationRequest(
                    "session-789",
                    customerIdIdentifier,
                    null,
                    Collections.emptyList()
            );

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-789")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your Social Security Number")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customerIdRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));
        }
    }
} 
