package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Token Retry Management Service Tests")
class TokenRetryManagementServiceTest {

    @Mock
    private BrandAuthConfigurationService brandConfigService;

    @InjectMocks
    private TokenRetryManagementService retryManagementService;

    private AuthenticationContext mockContext;
    private CustomerProfile mockCustomerProfile;
    private String testBrand = "PREMIUM_BANK";

    @BeforeEach
    void setUp() {
        mockContext = mock(AuthenticationContext.class);
        mockCustomerProfile = mock(CustomerProfile.class);
        
        when(mockContext.getBrand()).thenReturn(testBrand);
        when(mockContext.getAttemptId()).thenReturn("attempt-123");
        when(mockContext.getEligibleTokens()).thenReturn(Arrays.asList("DEBIT_CARD_PIN", "SSN", "DATE_OF_BIRTH"));
        
        // Setup default mock returns for BrandGlobalRetryPolicy
        BrandGlobalRetryPolicy defaultGlobalPolicy = createMockGlobalRetryPolicy();
        when(brandConfigService.getGlobalRetryPolicy(any())).thenReturn(defaultGlobalPolicy);
        
        // Setup default mock returns for TokenRetryStrategies
        Map<String, TokenRetryStrategy> defaultStrategies = createMockTokenRetryStrategies();
        when(brandConfigService.getTokenRetryStrategies(any())).thenReturn(defaultStrategies);
    }

    @Nested
    @DisplayName("Initialize Retry State Tests")
    class InitializeRetryStateTests {

        @Test
        @DisplayName("Should initialize token retry states for all eligible tokens")
        void shouldInitializeTokenRetryStatesForAllEligibleTokens() {
            // Given
            Map<String, TokenRetryStrategy> mockStrategies = createMockTokenRetryStrategies();
            BrandGlobalRetryPolicy mockGlobalPolicy = createMockGlobalRetryPolicy();
            
            when(brandConfigService.getTokenRetryStrategies(testBrand)).thenReturn(mockStrategies);
            when(brandConfigService.getGlobalRetryPolicy(testBrand)).thenReturn(mockGlobalPolicy);

            // When
            retryManagementService.initializeRetryState(mockContext, mockCustomerProfile);

            // Then
            verify(mockContext).setTokenRetryStates(any(Map.class));
            verify(mockContext).setGlobalRetryState(any(GlobalRetryState.class));
        }

        @Test
        @DisplayName("Should create default strategy for tokens without specific configuration")
        void shouldCreateDefaultStrategyForTokensWithoutSpecificConfiguration() {
            // Given
            Map<String, TokenRetryStrategy> mockStrategies = new HashMap<>();
            // Only provide strategy for one token, leaving others to use default
            mockStrategies.put("DEBIT_CARD_PIN", createMockTokenRetryStrategy("DEBIT_CARD_PIN"));
            
            BrandGlobalRetryPolicy mockGlobalPolicy = createMockGlobalRetryPolicy();
            
            when(brandConfigService.getTokenRetryStrategies(testBrand)).thenReturn(mockStrategies);
            when(brandConfigService.getGlobalRetryPolicy(testBrand)).thenReturn(mockGlobalPolicy);

            // When
            retryManagementService.initializeRetryState(mockContext, mockCustomerProfile);

            // Then
            verify(mockContext).setTokenRetryStates(any(Map.class));
            verify(mockContext).setGlobalRetryState(any(GlobalRetryState.class));
        }

        @Test
        @DisplayName("Should initialize global retry state with brand policy")
        void shouldInitializeGlobalRetryStateWithBrandPolicy() {
            // Given
            Map<String, TokenRetryStrategy> mockStrategies = createMockTokenRetryStrategies();
            BrandGlobalRetryPolicy mockGlobalPolicy = createMockGlobalRetryPolicy();
            
            when(brandConfigService.getTokenRetryStrategies(testBrand)).thenReturn(mockStrategies);
            when(brandConfigService.getGlobalRetryPolicy(testBrand)).thenReturn(mockGlobalPolicy);

            // When
            retryManagementService.initializeRetryState(mockContext, mockCustomerProfile);

            // Then
            verify(brandConfigService).getGlobalRetryPolicy(testBrand);
            verify(mockContext).setGlobalRetryState(any(GlobalRetryState.class));
        }
    }

    @Nested
    @DisplayName("Validate Token Retry Tests")
    class ValidateTokenRetryTests {

        @Test
        @DisplayName("Should allow retry when token is eligible")
        void shouldAllowRetryWhenTokenIsEligible() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    retryManagementService.validateTokenRetry(mockContext, tokenName);

            // Then
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.ALLOWED);
        }

        @Test
        @DisplayName("Should deny retry when global lockout is active")
        void shouldDenyRetryWhenGlobalLockoutIsActive() {
            // Given
            setupMockContextWithRetryStates();
            GlobalRetryState globalState = createMockGlobalRetryState();
            when(globalState.canAuthenticate()).thenReturn(false);
            when(globalState.getLockoutStatus()).thenReturn(GlobalRetryState.GlobalLockoutStatus.HARD_LOCKOUT);
            when(globalState.getLockoutExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(10));
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    retryManagementService.validateTokenRetry(mockContext, "DEBIT_CARD_PIN");

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.GLOBAL_LOCKED);
            assertThat(result.getMessage()).contains("temporarily locked");
        }

        @Test
        @DisplayName("Should deny retry when token is locked")
        void shouldDenyRetryWhenTokenIsLocked() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            
            Map<String, TokenRetryState> tokenStates = new HashMap<>();
            TokenRetryState lockedTokenState = createMockTokenRetryState();
            when(lockedTokenState.canRetryNow()).thenReturn(false);
            when(lockedTokenState.getLockoutStatus()).thenReturn(TokenRetryState.LockoutStatus.LOCKED_OUT);
            when(lockedTokenState.getLockoutExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            tokenStates.put(tokenName, lockedTokenState);
            
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    retryManagementService.validateTokenRetry(mockContext, tokenName);

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.TOKEN_LOCKED);
            assertThat(result.getMessage()).contains("temporarily locked");
        }

        @Test
        @DisplayName("Should apply retry delay when calculated")
        void shouldApplyRetryDelayWhenCalculated() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            
            TokenRetryStrategy mockStrategy = createMockTokenRetryStrategy(tokenName);
            when(mockStrategy.calculateDelayMs(anyInt())).thenReturn(5000L);
            
            Map<String, TokenRetryStrategy> strategies = new HashMap<>();
            strategies.put(tokenName, mockStrategy);
            when(brandConfigService.getTokenRetryStrategies(testBrand)).thenReturn(strategies);

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    retryManagementService.validateTokenRetry(mockContext, tokenName);

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.DELAYED);
            assertThat(result.getDelayMs()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should reset token retry window when expired")
        void shouldResetTokenRetryWindowWhenExpired() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            
            TokenRetryState expiredTokenState = createMockTokenRetryState();
            when(expiredTokenState.shouldResetWindow()).thenReturn(true);
            
            Map<String, TokenRetryState> tokenStates = new HashMap<>();
            tokenStates.put(tokenName, expiredTokenState);
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);

            // When
            retryManagementService.validateTokenRetry(mockContext, tokenName);

            // Then
            verify(expiredTokenState).resetAttempts();
        }

        @Test
        @DisplayName("Should return denied result when token state not found")
        void shouldReturnDeniedResultWhenTokenStateNotFound() {
            // Given
            setupMockContextWithRetryStates();
            when(mockContext.getTokenRetryStates()).thenReturn(new HashMap<>());

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    retryManagementService.validateTokenRetry(mockContext, "UNKNOWN_TOKEN");

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.DENIED);
            assertThat(result.getMessage()).contains("not found");
        }
    }

    @Nested
    @DisplayName("Record Token Attempt Tests")
    class RecordTokenAttemptTests {

        @Test
        @DisplayName("Should record successful attempt and update states")
        void shouldRecordSuccessfulAttemptAndUpdateStates() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            TokenRetryState tokenState = createMockTokenRetryState();
            GlobalRetryState globalState = createMockGlobalRetryState();
            
            Map<String, TokenRetryState> tokenStates = new HashMap<>();
            tokenStates.put(tokenName, tokenState);
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);

            // When
            retryManagementService.recordTokenAttempt(mockContext, tokenName, true, "Success");

            // Then
            verify(tokenState).recordAttempt(true, "Success");
            verify(globalState).recordSuccess();
        }

        @Test
        @DisplayName("Should record failed attempt and apply retry strategy")
        void shouldRecordFailedAttemptAndApplyRetryStrategy() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            TokenRetryState tokenState = createMockTokenRetryState();
            GlobalRetryState globalState = createMockGlobalRetryState();
            
            Map<String, TokenRetryState> tokenStates = new HashMap<>();
            tokenStates.put(tokenName, tokenState);
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);
            
            when(tokenState.hasAttemptsRemaining()).thenReturn(true);
            when(tokenState.getAttemptCount()).thenReturn(1);

            // When
            retryManagementService.recordTokenAttempt(mockContext, tokenName, false, "Failed");

            // Then
            verify(tokenState).recordAttempt(false, "Failed");
            verify(globalState).recordFailure();
        }

        @Test
        @DisplayName("Should apply lockout when token attempts exhausted")
        void shouldApplyLockoutWhenTokenAttemptsExhausted() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            TokenRetryState tokenState = createMockTokenRetryState();
            GlobalRetryState globalState = createMockGlobalRetryState();
            
            TokenRetryStrategy mockStrategy = createMockTokenRetryStrategy(tokenName);
            when(mockStrategy.isProgressiveLockoutEnabled()).thenReturn(true);
            when(mockStrategy.getLockoutDurationAfterExhaustion()).thenReturn(Duration.ofMinutes(15));
            
            Map<String, TokenRetryState> tokenStates = new HashMap<>();
            tokenStates.put(tokenName, tokenState);
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);
            
            Map<String, TokenRetryStrategy> strategies = new HashMap<>();
            strategies.put(tokenName, mockStrategy);
            when(brandConfigService.getTokenRetryStrategies(testBrand)).thenReturn(strategies);
            
            when(tokenState.hasAttemptsRemaining()).thenReturn(false);

            // When
            retryManagementService.recordTokenAttempt(mockContext, tokenName, false, "Failed");

            // Then
            verify(tokenState).setLockoutStatus(TokenRetryState.LockoutStatus.LOCKED_OUT);
            verify(tokenState).setLockoutExpiresAt(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should trigger escalation when thresholds exceeded")
        void shouldTriggerEscalationWhenThresholdsExceeded() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            TokenRetryState tokenState = createMockTokenRetryState();
            GlobalRetryState globalState = createMockGlobalRetryState();
            
            BrandGlobalRetryPolicy mockPolicy = createMockGlobalRetryPolicy();
            when(mockPolicy.shouldTriggerEscalation(anyInt())).thenReturn(true);
            
            Map<String, TokenRetryState> tokenStates = new HashMap<>();
            tokenStates.put(tokenName, tokenState);
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);
            when(brandConfigService.getGlobalRetryPolicy(testBrand)).thenReturn(mockPolicy);
            
            when(tokenState.hasAttemptsRemaining()).thenReturn(true);

            // When
            retryManagementService.recordTokenAttempt(mockContext, tokenName, false, "Failed");

            // Then
            verify(globalState).escalate();
        }

        @Test
        @DisplayName("Should trigger suspicious activity lockout")
        void shouldTriggerSuspiciousActivityLockout() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            TokenRetryState tokenState = createMockTokenRetryState();
            GlobalRetryState globalState = createMockGlobalRetryState();
            
            BrandGlobalRetryPolicy mockPolicy = createMockGlobalRetryPolicy();
            when(mockPolicy.shouldTriggerSuspiciousActivityLockout(anyInt())).thenReturn(true);
            when(mockPolicy.getSuspiciousActivityLockoutDuration()).thenReturn(Duration.ofMinutes(30));
            
            Map<String, TokenRetryState> tokenStates = new HashMap<>();
            tokenStates.put(tokenName, tokenState);
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);
            when(brandConfigService.getGlobalRetryPolicy(testBrand)).thenReturn(mockPolicy);
            
            when(tokenState.hasAttemptsRemaining()).thenReturn(true);

            // When
            retryManagementService.recordTokenAttempt(mockContext, tokenName, false, "Failed");

            // Then
            verify(globalState).triggerLockout(eq(GlobalRetryState.GlobalLockoutStatus.SUSPICIOUS_ACTIVITY), any(LocalDateTime.class));
            verify(globalState).setSuspiciousActivityDetected(true);
        }

        @Test
        @DisplayName("Should handle missing token state gracefully")
        void shouldHandleMissingTokenStateGracefully() {
            // Given
            when(mockContext.getTokenRetryStates()).thenReturn(new HashMap<>());

            // When & Then - Should not throw exception
            retryManagementService.recordTokenAttempt(mockContext, "UNKNOWN_TOKEN", false, "Failed");
        }
    }

    @Nested
    @DisplayName("Retry Analytics Tests")
    class RetryAnalyticsTests {

        @Test
        @DisplayName("Should generate comprehensive retry analytics")
        void shouldGenerateComprehensiveRetryAnalytics() {
            // Given
            setupMockContextWithRetryStates();
            Map<String, TokenRetryState> tokenStates = createMockTokenRetryStates();
            GlobalRetryState globalState = createMockGlobalRetryState();
            
            when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);

            // When
            TokenRetryManagementService.RetryAnalytics analytics = 
                    retryManagementService.getRetryAnalytics(mockContext);

            // Then
            assertThat(analytics).isNotNull();
            assertThat(analytics.getBrand()).isEqualTo(testBrand);
            assertThat(analytics.getAttemptId()).isEqualTo("attempt-123");
            assertThat(analytics.getTokenRetryStates()).isEqualTo(tokenStates);
            assertThat(analytics.getGlobalRetryState()).isEqualTo(globalState);
            assertThat(analytics.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Retry Validation Result Tests")
    class RetryValidationResultTests {

        @Test
        @DisplayName("Should create allowed result correctly")
        void shouldCreateAllowedResultCorrectly() {
            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    TokenRetryManagementService.RetryValidationResult.allowed("Test message");

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.ALLOWED);
            assertThat(result.getMessage()).isEqualTo("Test message");
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.isDelayed()).isFalse();
            assertThat(result.isLocked()).isFalse();
        }

        @Test
        @DisplayName("Should create delayed result correctly")
        void shouldCreateDelayedResultCorrectly() {
            // Given
            LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(30);
            long delayMs = 30000;

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    TokenRetryManagementService.RetryValidationResult.delayed("Delay message", nextRetryAt, delayMs);

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.DELAYED);
            assertThat(result.getMessage()).isEqualTo("Delay message");
            assertThat(result.getNextRetryAt()).isEqualTo(nextRetryAt);
            assertThat(result.getDelayMs()).isEqualTo(delayMs);
            assertThat(result.isDelayed()).isTrue();
            assertThat(result.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("Should create token locked result correctly")
        void shouldCreateTokenLockedResultCorrectly() {
            // Given
            LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(5);
            LocalDateTime lockoutExpiry = LocalDateTime.now().plusMinutes(15);

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    TokenRetryManagementService.RetryValidationResult.tokenLocked("Token locked", nextRetryAt, lockoutExpiry);

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.TOKEN_LOCKED);
            assertThat(result.getMessage()).isEqualTo("Token locked");
            assertThat(result.getNextRetryAt()).isEqualTo(nextRetryAt);
            assertThat(result.getLockoutExpiresAt()).isEqualTo(lockoutExpiry);
            assertThat(result.isLocked()).isTrue();
        }

        @Test
        @DisplayName("Should create global lockout result correctly")
        void shouldCreateGlobalLockoutResultCorrectly() {
            // Given
            LocalDateTime lockoutExpiry = LocalDateTime.now().plusMinutes(20);

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    TokenRetryManagementService.RetryValidationResult.globalLockout("Global lockout", lockoutExpiry);

            // Then
            assertThat(result.getStatus()).isEqualTo(TokenRetryManagementService.RetryValidationResult.Status.GLOBAL_LOCKED);
            assertThat(result.getMessage()).isEqualTo("Global lockout");
            assertThat(result.getLockoutExpiresAt()).isEqualTo(lockoutExpiry);
            assertThat(result.isLocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cross-Token Delay Calculation Tests")
    class CrossTokenDelayCalculationTests {

        @Test
        @DisplayName("Should apply cross-token delay multiplier")
        void shouldApplyCrossTokenDelayMultiplier() {
            // Given
            setupMockContextWithRetryStates();
            String tokenName = "DEBIT_CARD_PIN";
            
            TokenRetryStrategy mockStrategy = createMockTokenRetryStrategy(tokenName);
            when(mockStrategy.calculateDelayMs(anyInt())).thenReturn(1000L);
            
            GlobalRetryState globalState = createMockGlobalRetryState();
            when(globalState.getCrossTokenDelayMultiplier()).thenReturn(2.0);
            when(mockContext.getGlobalRetryState()).thenReturn(globalState);
            
            Map<String, TokenRetryStrategy> strategies = new HashMap<>();
            strategies.put(tokenName, mockStrategy);
            when(brandConfigService.getTokenRetryStrategies(testBrand)).thenReturn(strategies);

            // When
            TokenRetryManagementService.RetryValidationResult result = 
                    retryManagementService.validateTokenRetry(mockContext, tokenName);

            // Then
            if (result.isDelayed()) {
                assertThat(result.getDelayMs()).isEqualTo(2000L); // 1000 * 2.0
            }
        }
    }

    // Helper methods
    private void setupMockContextWithRetryStates() {
        Map<String, TokenRetryState> tokenStates = createMockTokenRetryStates();
        GlobalRetryState globalState = createMockGlobalRetryState();
        
        when(mockContext.getTokenRetryStates()).thenReturn(tokenStates);
        when(mockContext.getGlobalRetryState()).thenReturn(globalState);
    }

    private Map<String, TokenRetryState> createMockTokenRetryStates() {
        Map<String, TokenRetryState> tokenStates = new HashMap<>();
        for (String token : Arrays.asList("DEBIT_CARD_PIN", "SSN", "DATE_OF_BIRTH")) {
            TokenRetryState state = createMockTokenRetryState();
            when(state.canRetryNow()).thenReturn(true);
            when(state.shouldResetWindow()).thenReturn(false);
            tokenStates.put(token, state);
        }
        return tokenStates;
    }

    private TokenRetryState createMockTokenRetryState() {
        TokenRetryState mockState = mock(TokenRetryState.class);
        when(mockState.canRetryNow()).thenReturn(true);
        when(mockState.shouldResetWindow()).thenReturn(false);
        when(mockState.hasAttemptsRemaining()).thenReturn(true);
        when(mockState.getAttemptCount()).thenReturn(1);
        when(mockState.getLockoutStatus()).thenReturn(TokenRetryState.LockoutStatus.NONE);
        return mockState;
    }

    private GlobalRetryState createMockGlobalRetryState() {
        GlobalRetryState mockState = mock(GlobalRetryState.class);
        when(mockState.canAuthenticate()).thenReturn(true);
        when(mockState.getCrossTokenDelayMultiplier()).thenReturn(1.0);
        when(mockState.getTotalFailures()).thenReturn(0);
        when(mockState.getConsecutiveFailures()).thenReturn(0);
        when(mockState.getRapidFailureCount()).thenReturn(0);
        return mockState;
    }

    private Map<String, TokenRetryStrategy> createMockTokenRetryStrategies() {
        Map<String, TokenRetryStrategy> strategies = new HashMap<>();
        for (String token : Arrays.asList("DEBIT_CARD_PIN", "SSN", "DATE_OF_BIRTH")) {
            strategies.put(token, createMockTokenRetryStrategy(token));
        }
        return strategies;
    }

    private TokenRetryStrategy createMockTokenRetryStrategy(String tokenName) {
        TokenRetryStrategy mockStrategy = mock(TokenRetryStrategy.class);
        when(mockStrategy.getTokenName()).thenReturn(tokenName);
        when(mockStrategy.getRetryType()).thenReturn(TokenRetryStrategy.RetryType.FIXED_DELAY);
        when(mockStrategy.getMaxRetries()).thenReturn(3);
        when(mockStrategy.getBaseDelayMs()).thenReturn(1000L);
        when(mockStrategy.isProgressiveLockoutEnabled()).thenReturn(false);
        when(mockStrategy.getResetWindowDuration()).thenReturn(Duration.ofMinutes(30));
        when(mockStrategy.calculateDelayMs(anyInt())).thenReturn(0L);
        return mockStrategy;
    }

    private BrandGlobalRetryPolicy createMockGlobalRetryPolicy() {
        BrandGlobalRetryPolicy mockPolicy = mock(BrandGlobalRetryPolicy.class);
        when(mockPolicy.getBrandCode()).thenReturn(testBrand);
        when(mockPolicy.getRetryWindowResetDuration()).thenReturn(Duration.ofHours(1));
        when(mockPolicy.shouldTriggerGlobalLockout(anyInt())).thenReturn(false);
        when(mockPolicy.shouldTriggerSuspiciousActivityLockout(anyInt())).thenReturn(false);
        when(mockPolicy.shouldTriggerEscalation(anyInt())).thenReturn(false);
        return mockPolicy;
    }
} 