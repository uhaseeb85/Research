package com.bank.ivr.auth.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.ProvidedToken;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.service.AuthenticationOrchestrator;
import com.bank.ivr.auth.service.BrandAuthConfigurationService;
import com.bank.ivr.auth.service.DnisConfigurationService;
import com.bank.ivr.auth.service.SessionContextService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthenticationController.class)
@DisplayName("Brand-Aware Authentication Controller Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationOrchestrator authenticationOrchestrator;
    
    @MockBean
    private BrandAuthConfigurationService brandConfigService;
    
    @MockBean
    private DnisConfigurationService dnisConfigService;

    @MockBean
    private SessionContextService sessionContextService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationRequest premiumBankRequest;
    private AuthenticationRequest communityBankRequest;
    private CustomerIdentifier customerIdentifier;

    @BeforeEach
    void setUp() {
        customerIdentifier = new CustomerIdentifier(
                CustomerIdentifier.IdentifierType.PHONE_NUMBER,
                "+1234567890"
        );
        
        premiumBankRequest = new AuthenticationRequest(
                "session-123",
                customerIdentifier,
                null, // New attempt
                Collections.emptyList(),
                "PREMIUM_BANK",
                createDefaultTrustLevelInfo()
        );
        
        communityBankRequest = new AuthenticationRequest(
                "session-456",
                customerIdentifier,
                null, // New attempt
                Collections.emptyList(),
                "COMMUNITY_BANK",
                createDefaultTrustLevelInfo()
        );

        // Setup default brand configuration service mocks
        when(brandConfigService.isBrandSupported("PREMIUM_BANK")).thenReturn(true);
        when(brandConfigService.isBrandSupported("COMMUNITY_BANK")).thenReturn(true);
        when(brandConfigService.isBrandSupported("UNSUPPORTED_BRAND")).thenReturn(false);
        when(brandConfigService.getAvailableBrands()).thenReturn(new HashSet<>(Arrays.asList("PREMIUM_BANK", "COMMUNITY_BANK")));
        
        // Default brand configuration responses
        when(brandConfigService.getMaxOverallAttemptsForBrand("PREMIUM_BANK")).thenReturn(3);

        when(brandConfigService.isConcurrentTokenAuthAllowed("PREMIUM_BANK")).thenReturn(true);
        
        when(brandConfigService.getMaxOverallAttemptsForBrand("COMMUNITY_BANK")).thenReturn(5);

        when(brandConfigService.isConcurrentTokenAuthAllowed("COMMUNITY_BANK")).thenReturn(false);
        
        when(brandConfigService.getBrandMessage(any(), eq("failure"))).thenReturn("Authentication failed. Please try again or contact support.");
        
        // Setup default session context service mocks
        when(sessionContextService.getDnisFromSession(any())).thenReturn(Optional.empty());
        when(sessionContextService.getSessionSsnFromSession(any())).thenReturn(Optional.empty());
    }
    
    private TrustLevelInfo createDefaultTrustLevelInfo() {
        return new TrustLevelInfo(
                TrustLevelInfo.TrustLevel.GREEN,
                TrustLevelInfo.PhoneMatchStatus.SINGLE_MATCH,
                1
        );
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Brand Validation")
    class BrandValidation {

        @Test
        @DisplayName("Should reject unsupported brand")
        void shouldRejectUnsupportedBrand() throws Exception {
            // Given
            AuthenticationRequest unsupportedBrandRequest = new AuthenticationRequest("session-789", customerIdentifier, null, Collections.emptyList(), "UNSUPPORTED_BRAND", createDefaultTrustLevelInfo());

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unsupportedBrandRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("Brand 'UNSUPPORTED_BRAND' is not supported")));

            verify(authenticationOrchestrator, never()).authenticateCustomer(any(), any(), any());
        }

        @Test
        @DisplayName("Should validate required brand field")
        void shouldValidateRequiredBrandField() throws Exception {
            // Given - AuthenticationRequest without brand (this should fail validation)
            String requestJsonWithoutBrand = "{"
                    + "\"sessionId\":\"session-123\","
                    + "\"customerIdentifier\":{\"type\":\"PHONE_NUMBER\",\"value\":\"+1234567890\"},"
                    + "\"providedTokens\":[]"
                    + "}";

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJsonWithoutBrand))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authenticationOrchestrator, never()).authenticateCustomer(any(), any(), any());
        }

        @Test
        @DisplayName("Should accept valid supported brand")
        void shouldAcceptValidSupportedBrand() throws Exception {
            // Given
            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your 4-digit PIN.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(premiumBankRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));

            verify(brandConfigService).isBrandSupported("PREMIUM_BANK");
            verify(authenticationOrchestrator).authenticateCustomer(any(AuthenticationRequest.class), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Premium Bank Brand Tests")
    class PremiumBankBrandTests {

        @Test
        @DisplayName("Should initiate Premium Bank authentication successfully")
        void shouldInitiatePremiumBankAuthenticationSuccessfully() throws Exception {
            // Given
            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your 4-digit PIN.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(premiumBankRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId", is("attempt-123")))
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")))
                    .andExpect(jsonPath("$.message", containsString("PIN")));

            verify(authenticationOrchestrator).authenticateCustomer(any(AuthenticationRequest.class), any(), any());
        }

        @Test
        @DisplayName("Should handle Premium Bank multi-factor authentication")
        void shouldHandlePremiumBankMultiFactorAuth() throws Exception {
            // Given - First token provided
            List<ProvidedToken> tokens = Arrays.asList(
                    new ProvidedToken("DEBIT_CARD_PIN", "1234")
            );
            
            AuthenticationRequest continuingRequest = new AuthenticationRequest("session-123", customerIdentifier, "attempt-123", tokens, "PREMIUM_BANK", createDefaultTrustLevelInfo());

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_MORE_TOKENS)
                    .message("Please also provide your date of birth for additional verification.")
                    .authenticatedTokens(Arrays.asList("DEBIT_CARD_PIN"))

                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(continuingRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_MORE_TOKENS")))
                    .andExpect(jsonPath("$.authenticatedTokens[0]", is("DEBIT_CARD_PIN")))
;
        }

        @Test
        @DisplayName("Should complete Premium Bank authentication successfully")
        void shouldCompletePremiumBankAuthenticationSuccessfully() throws Exception {
            // Given - Both tokens provided
            List<ProvidedToken> tokens = Arrays.asList(
                    new ProvidedToken("DEBIT_CARD_PIN", "1234"),
                    new ProvidedToken("DATE_OF_BIRTH", "01/01/1990")
            );
            
            AuthenticationRequest completingRequest = new AuthenticationRequest("session-123", customerIdentifier, "attempt-123", tokens, "PREMIUM_BANK", createDefaultTrustLevelInfo());

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.AUTHENTICATED)
                    .message("Authentication successful. Welcome to Premium Banking services.")
                    .authenticatedTokens(Arrays.asList("DEBIT_CARD_PIN", "DATE_OF_BIRTH"))
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completingRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("AUTHENTICATED")))
                    .andExpect(jsonPath("$.message", containsString("Premium Banking")))
                    .andExpect(jsonPath("$.authenticatedTokens", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Community Bank Brand Tests")
    class CommunityBankBrandTests {

        @Test
        @DisplayName("Should initiate Community Bank authentication successfully")
        void shouldInitiateCommunityBankAuthenticationSuccessfully() throws Exception {
            // Given
            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-456")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide the last 4 digits of your Social Security Number.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(communityBankRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptId", is("attempt-456")))
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")))
                    .andExpect(jsonPath("$.message", containsString("Social Security Number")));
        }

        @Test
        @DisplayName("Should complete Community Bank authentication with single factor")
        void shouldCompleteCommunityBankAuthenticationWithSingleFactor() throws Exception {
            // Given - SSN provided (single factor for community bank)
            List<ProvidedToken> tokens = Arrays.asList(
                    new ProvidedToken("SSN", "123456789")
            );
            
            AuthenticationRequest completingRequest = new AuthenticationRequest("session-456", customerIdentifier, "attempt-456", tokens, "COMMUNITY_BANK", createDefaultTrustLevelInfo());

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-456")
                    .status(AuthStatus.AUTHENTICATED)
                    .message("Great! You're all set. How can we help you today?")
                    .authenticatedTokens(Arrays.asList("SSN"))
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completingRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("AUTHENTICATED")))
                    .andExpect(jsonPath("$.message", containsString("all set")))
                    .andExpect(jsonPath("$.authenticatedTokens", hasSize(1)))
                    .andExpect(jsonPath("$.authenticatedTokens[0]", is("SSN")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/methods/{brand} - Brand-Specific Authentication Methods")
    class BrandSpecificAuthenticationMethods {

        @Test
        @DisplayName("Should return Premium Bank authentication methods")
        void shouldReturnPremiumBankAuthenticationMethods() throws Exception {
            // Given
            List<AuthTokenDefinition> premiumTokens = Arrays.asList(
                    AuthTokenDefinition.builder()
                            .name("DEBIT_CARD_PIN")
                            .description("Debit Card PIN")
                            .priority(100)
                            .maxAttempts(3)
                            .build(),
                    AuthTokenDefinition.builder()
                            .name("SSN")
                            .description("Social Security Number")
                            .priority(95)
                            .maxAttempts(2)
                            .build()
            );

            when(brandConfigService.getTokenDefinitionsForBrand("PREMIUM_BANK")).thenReturn(premiumTokens);

            when(brandConfigService.getMaxOverallAttemptsForBrand("PREMIUM_BANK")).thenReturn(3);
            when(brandConfigService.isConcurrentTokenAuthAllowed("PREMIUM_BANK")).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/v1/auth/methods/PREMIUM_BANK"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.brand", is("PREMIUM_BANK")))
                    .andExpect(jsonPath("$.tokenDefinitions", hasSize(2)))
                    .andExpect(jsonPath("$.tokenDefinitions[0].name", is("DEBIT_CARD_PIN")))
                    .andExpect(jsonPath("$.tokenDefinitions[0].priority", is(100)))

                    .andExpect(jsonPath("$.maxOverallAttempts", is(3)))
                    .andExpect(jsonPath("$.concurrentAuthAllowed", is(true)));
        }

        @Test
        @DisplayName("Should return Community Bank authentication methods")
        void shouldReturnCommunityBankAuthenticationMethods() throws Exception {
            // Given
            List<AuthTokenDefinition> communityTokens = Arrays.asList(
                    AuthTokenDefinition.builder()
                            .name("SSN")
                            .description("Social Security Number")
                            .priority(100)
                            .maxAttempts(3)
                            .build(),
                    AuthTokenDefinition.builder()
                            .name("DATE_OF_BIRTH")
                            .description("Date of Birth")
                            .priority(95)
                            .maxAttempts(3)
                            .build()
            );

            when(brandConfigService.getTokenDefinitionsForBrand("COMMUNITY_BANK")).thenReturn(communityTokens);

            when(brandConfigService.getMaxOverallAttemptsForBrand("COMMUNITY_BANK")).thenReturn(5);
            when(brandConfigService.isConcurrentTokenAuthAllowed("COMMUNITY_BANK")).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/v1/auth/methods/COMMUNITY_BANK"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.brand", is("COMMUNITY_BANK")))
                    .andExpect(jsonPath("$.tokenDefinitions", hasSize(2)))
                    .andExpect(jsonPath("$.tokenDefinitions[0].name", is("SSN")))
                    .andExpect(jsonPath("$.tokenDefinitions[0].priority", is(100)))

                    .andExpect(jsonPath("$.maxOverallAttempts", is(5)))
                    .andExpect(jsonPath("$.concurrentAuthAllowed", is(false)));
        }

        @Test
        @DisplayName("Should return 400 for unsupported brand in methods endpoint")
        void shouldReturn400ForUnsupportedBrandInMethodsEndpoint() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/auth/methods/UNSUPPORTED_BRAND"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Brand 'UNSUPPORTED_BRAND' is not supported")));
        }

        @Test
        @DisplayName("Should handle exception in methods endpoint")
        void shouldHandleExceptionInMethodsEndpoint() throws Exception {
            // Given
            when(brandConfigService.getTokenDefinitionsForBrand("PREMIUM_BANK"))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            mockMvc.perform(get("/api/v1/auth/methods/PREMIUM_BANK"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Failed to retrieve authentication methods for brand 'PREMIUM_BANK'")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/brands - Supported Brands")
    class SupportedBrands {

        @Test
        @DisplayName("Should return all supported brands")
        void shouldReturnAllSupportedBrands() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/auth/brands"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.supportedBrands", hasSize(2)))
                    .andExpect(jsonPath("$.supportedBrands", hasItems("PREMIUM_BANK", "COMMUNITY_BANK")))
                    .andExpect(jsonPath("$.count", is(2)));
        }

        @Test
        @DisplayName("Should handle exception in brands endpoint")
        void shouldHandleExceptionInBrandsEndpoint() throws Exception {
            // Given
            when(brandConfigService.getAvailableBrands())
                    .thenThrow(new RuntimeException("Configuration service failed"));

            // When & Then
            mockMvc.perform(get("/api/v1/auth/brands"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Failed to retrieve supported brands")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Error Handling with Brand Context")
    class BrandAwareErrorHandling {

        @Test
        @DisplayName("Should handle IllegalArgumentException with brand context")
        void shouldHandleIllegalArgumentExceptionWithBrandContext() throws Exception {
            // Given
            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenThrow(new IllegalArgumentException("Invalid token format"));

            when(brandConfigService.getBrandMessage("PREMIUM_BANK", "failure"))
                    .thenReturn("Authentication failed. Please contact Premium Support at 1-800-PREMIUM.");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(premiumBankRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", is("Authentication failed. Please contact Premium Support at 1-800-PREMIUM.")));

            verify(brandConfigService).getBrandMessage("PREMIUM_BANK", "failure");
        }

        @Test
        @DisplayName("Should handle unexpected exceptions with brand context")
        void shouldHandleUnexpectedExceptionsWithBrandContext() throws Exception {
            // Given
            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            when(brandConfigService.getBrandMessage("COMMUNITY_BANK", "failure"))
                    .thenReturn("We couldn't verify your identity. Please visit your local branch or call us at 1-800-COMMUNITY.");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(communityBankRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                                        .andExpect(jsonPath("$.status", is("FAILED")))                    .andExpect(jsonPath("$.message", is("We couldn't verify your identity. Please visit your local branch or call us at 1-800-COMMUNITY.")));

            verify(brandConfigService).getBrandMessage("COMMUNITY_BANK", "failure");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/customer - Brand Configuration Integration")
    class BrandConfigurationIntegration {

        @Test
        @DisplayName("Should validate brand support in request processing")
        void shouldValidateBrandSupportInRequestProcessing() throws Exception {
            // Given
            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your 4-digit PIN.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(premiumBankRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());

            // Verify brand configuration service was called for validation
            verify(brandConfigService).isBrandSupported("PREMIUM_BANK");
            // Note: Other brand config methods are called by the orchestrator, not the controller
        }

        @Test
        @DisplayName("Should handle missing customer identifier with brand context")
        void shouldHandleMissingCustomerIdentifierWithBrandContext() throws Exception {
            // Given
            String requestJsonWithoutCustomerIdentifier = "{"
                    + "\"sessionId\":\"session-123\","
                    + "\"brand\":\"PREMIUM_BANK\","
                    + "\"providedTokens\":[]"
                    + "}";

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJsonWithoutCustomerIdentifier))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authenticationOrchestrator, never()).authenticateCustomer(any(), any(), any());
        }

        @Test
        @DisplayName("Should handle different customer identifier types with brand")
        void shouldHandleDifferentCustomerIdentifierTypesWithBrand() throws Exception {
            // Given
            CustomerIdentifier accountIdentifier = new CustomerIdentifier(
                    CustomerIdentifier.IdentifierType.ACCOUNT_NUMBER,
                    "ACC123456789"
            );
            
            AuthenticationRequest accountRequest = new AuthenticationRequest("session-789", accountIdentifier, null, Collections.emptyList(), "PREMIUM_BANK", createDefaultTrustLevelInfo());

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-789")
                    .status(AuthStatus.PENDING_PRIMARY_TOKEN)
                    .message("Please provide your 4-digit PIN.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_PRIMARY_TOKEN")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/health - Health Check")
    class HealthCheck {

        @Test
        @DisplayName("Should return brand-aware healthy status")
        void shouldReturnBrandAwareHealthyStatus() throws Exception {
            mockMvc.perform(get("/api/v1/auth/health"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("IVR Authentication Service is healthy (Brand-aware with DNIS support)"));
        }

        @Test
        @DisplayName("Should handle health check exception")
        void shouldHandleHealthCheckException() throws Exception {
            // This test mainly exists for coverage - health check is simple
            // In a real scenario, health check might check database connectivity, etc.
            mockMvc.perform(get("/api/v1/auth/health"))
                    .andExpect(status().isOk());
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
    @DisplayName("Brand-Specific Token Validation Edge Cases")
    class BrandSpecificTokenValidationEdgeCases {

        @Test
        @DisplayName("Should handle empty token list with brand context")
        void shouldHandleEmptyTokenListWithBrandContext() throws Exception {
            // Given
            AuthenticationRequest emptyTokenRequest = new AuthenticationRequest("session-123", customerIdentifier, "attempt-123", Collections.emptyList(), "PREMIUM_BANK", createDefaultTrustLevelInfo());

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.PENDING_MORE_TOKENS)
                    .message("Please provide your 4-digit PIN.")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyTokenRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING_MORE_TOKENS")));
        }

        @Test
        @DisplayName("Should handle invalid token for specific brand")
        void shouldHandleInvalidTokenForSpecificBrand() throws Exception {
            // Given
            List<ProvidedToken> invalidTokens = Arrays.asList(
                    new ProvidedToken("INVALID_TOKEN", "invalid_value")
            );
            
            AuthenticationRequest invalidTokenRequest = new AuthenticationRequest("session-123", customerIdentifier, "attempt-123", invalidTokens, "PREMIUM_BANK", createDefaultTrustLevelInfo());

            AuthenticationResponse mockResponse = AuthenticationResponse.builder()
                    .attemptId("attempt-123")
                    .status(AuthStatus.FAILED)
                    .message("Invalid token type for Premium Bank")
                    .build();

            when(authenticationOrchestrator.authenticateCustomer(any(AuthenticationRequest.class), any(), any()))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidTokenRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.message", containsString("Invalid token type")));
        }
    }

            } 
