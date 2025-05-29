package com.bank.ivr.auth.config.impl;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;
import com.bank.ivr.auth.model.domain.TokenRetryStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Premium Bank Authentication Configuration Tests")
class PremiumBankAuthConfigurationTest {

    private PremiumBankAuthConfiguration config;

    @BeforeEach
    void setUp() {
        config = new PremiumBankAuthConfiguration();
    }

    @Nested
    @DisplayName("Basic Configuration Tests")
    class BasicConfigurationTests {

        @Test
        @DisplayName("Should return correct brand code")
        void shouldReturnCorrectBrandCode() {
            // When
            String brandCode = config.getBrandCode();

            // Then
            assertThat(brandCode).isEqualTo("PREMIUM_BANK");
        }

        @Test
        @DisplayName("Should return high priority")
        void shouldReturnHighPriority() {
            // When
            int priority = config.getPriority();

            // Then
            assertThat(priority).isEqualTo(100);
        }

        @Test
        @DisplayName("Should return strict max overall attempts")
        void shouldReturnStrictMaxOverallAttempts() {
            // When
            int maxAttempts = config.getMaxOverallAttempts();

            // Then
            assertThat(maxAttempts).isEqualTo(3);
        }

        @Test
        @DisplayName("Should allow concurrent token authentication")
        void shouldAllowConcurrentTokenAuthentication() {
            // When
            boolean concurrentAllowed = config.isConcurrentTokenAuthAllowed();

            // Then
            assertThat(concurrentAllowed).isTrue();
        }
    }

    @Nested
    @DisplayName("Token Definitions Tests")
    class TokenDefinitionsTests {

        @Test
        @DisplayName("Should return all expected token definitions")
        void shouldReturnAllExpectedTokenDefinitions() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            assertThat(tokenDefinitions).hasSize(5);
            
            List<String> tokenNames = new ArrayList<String>();
            for (AuthTokenDefinition token : tokenDefinitions) {
                tokenNames.add(token.getName());
            }
            
            assertThat(tokenNames).containsExactlyInAnyOrder(
                    "DEBIT_CARD_PIN", "SSN", "DATE_OF_BIRTH", 
                    "MOTHER_MAIDEN_NAME", "VOICE_BIOMETRIC"
            );
        }

        @Test
        @DisplayName("Should have debit card PIN as highest priority token")
        void shouldHaveDebitCardPINAsHighestPriorityToken() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            AuthTokenDefinition pinToken = null;
            for (AuthTokenDefinition token : tokenDefinitions) {
                if ("DEBIT_CARD_PIN".equals(token.getName())) {
                    pinToken = token;
                    break;
                }
            }
            assertThat(pinToken).isNotNull();

            assertThat(pinToken.getPriority()).isEqualTo(100);
            assertThat(pinToken.getDescription()).isEqualTo("Debit Card PIN");
            assertThat(pinToken.getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should have SSN with stricter limits")
        void shouldHaveSSNWithStricterLimits() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            AuthTokenDefinition ssnToken = null;
            for (AuthTokenDefinition token : tokenDefinitions) {
                if ("SSN".equals(token.getName())) {
                    ssnToken = token;
                    break;
                }
            }
            assertThat(ssnToken).isNotNull();

            assertThat(ssnToken.getPriority()).isEqualTo(95);
            assertThat(ssnToken.getMaxAttempts()).isEqualTo(2); // Stricter than community bank
        }

        @Test
        @DisplayName("Should have premium voice biometric token")
        void shouldHavePremiumVoiceBiometricToken() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            AuthTokenDefinition voiceToken = null;
            for (AuthTokenDefinition token : tokenDefinitions) {
                if ("VOICE_BIOMETRIC".equals(token.getName())) {
                    voiceToken = token;
                    break;
                }
            }
            assertThat(voiceToken).isNotNull();

            assertThat(voiceToken.getPriority()).isEqualTo(80);
            assertThat(voiceToken.getDescription()).isEqualTo("Voice Authentication");
            assertThat(voiceToken.getMaxAttempts()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should have mother maiden name with stricter limits")
        void shouldHaveMotherMaidenNameWithStricterLimits() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            AuthTokenDefinition maidenNameToken = null;
            for (AuthTokenDefinition token : tokenDefinitions) {
                if ("MOTHER_MAIDEN_NAME".equals(token.getName())) {
                    maidenNameToken = token;
                    break;
                }
            }
            assertThat(maidenNameToken).isNotNull();

            assertThat(maidenNameToken.getMaxAttempts()).isEqualTo(2); // Stricter than community bank
        }

        @Test
        @DisplayName("Should have tokens ordered by priority")
        void shouldHaveTokensOrderedByPriority() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            for (int i = 1; i < tokenDefinitions.size(); i++) {
                int currentPriority = tokenDefinitions.get(i).getPriority();
                int previousPriority = tokenDefinitions.get(i - 1).getPriority();
                assertThat(currentPriority).isLessThanOrEqualTo(previousPriority);
            }
        }
    }

    @Nested
    @DisplayName("Brand Specific Token Attempts Tests")
    class BrandSpecificTokenAttemptsTests {

        @Test
        @DisplayName("Should return strict token attempts mapping")
        void shouldReturnStrictTokenAttemptsMapping() {
            // When
            Map<String, Integer> tokenAttempts = config.getBrandSpecificTokenAttempts();

            // Then
            assertThat(tokenAttempts).hasSize(5);
            assertThat(tokenAttempts.get("SSN")).isEqualTo(2); // Stricter
            assertThat(tokenAttempts.get("DEBIT_CARD_PIN")).isEqualTo(3);
            assertThat(tokenAttempts.get("DATE_OF_BIRTH")).isEqualTo(3);
            assertThat(tokenAttempts.get("MOTHER_MAIDEN_NAME")).isEqualTo(2); // Stricter
            assertThat(tokenAttempts.get("VOICE_BIOMETRIC")).isEqualTo(2); // Stricter
        }
    }

    @Nested
    @DisplayName("Brand Messages Tests")
    class BrandMessagesTests {

        @Test
        @DisplayName("Should return all required brand messages")
        void shouldReturnAllRequiredBrandMessages() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            assertThat(messages).containsKeys(
                    "welcome", "primary_prompt", "secondary_prompt", "success", 
                    "failure", "customer_not_found", "session_expired", 
                    "system_error", "no_methods"
            );
        }

        @Test
        @DisplayName("Should have premium-focused welcome message")
        void shouldHavePremiumFocusedWelcomeMessage() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            String welcomeMessage = messages.get("welcome");
            assertThat(welcomeMessage).contains("Premium Bank");
            assertThat(welcomeMessage).contains("enhanced authentication");
        }

        @Test
        @DisplayName("Should have premium support failure message")
        void shouldHavePremiumSupportFailureMessage() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            String failureMessage = messages.get("failure");
            assertThat(failureMessage).contains("Premium Support");
            assertThat(failureMessage).contains("1-800-PREMIUM");
        }

        @Test
        @DisplayName("Should have security-focused success message")
        void shouldHaveSecurityFocusedSuccessMessage() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            String successMessage = messages.get("success");
            assertThat(successMessage).contains("Premium Banking services");
        }

        @Test
        @DisplayName("Should have parameterized prompt messages")
        void shouldHaveParameterizedPromptMessages() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            assertThat(messages.get("primary_prompt")).contains("{token_description}");
            assertThat(messages.get("secondary_prompt")).contains("{token_description}");
        }
    }

    @Nested
    @DisplayName("Token Retry Strategies Tests")
    class TokenRetryStrategiesTests {

        @Test
        @DisplayName("Should return retry strategies for configured tokens")
        void shouldReturnRetryStrategiesForConfiguredTokens() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            assertThat(strategies).hasSize(3);
            assertThat(strategies).containsKeys("DEBIT_CARD_PIN", "SSN", "VOICE_BIOMETRIC");
        }

        @Test
        @DisplayName("Should use exponential backoff for debit card PIN")
        void shouldUseExponentialBackoffForDebitCardPIN() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            TokenRetryStrategy pinStrategy = strategies.get("DEBIT_CARD_PIN");
            assertThat(pinStrategy.getTokenName()).isEqualTo("DEBIT_CARD_PIN");
            assertThat(pinStrategy.getRetryType()).isEqualTo(TokenRetryStrategy.RetryType.EXPONENTIAL_BACKOFF);
            assertThat(pinStrategy.getMaxRetries()).isEqualTo(2);
            assertThat(pinStrategy.getBaseDelayMs()).isEqualTo(5000);
            assertThat(pinStrategy.getMaxDelayMs()).isEqualTo(60000);
            assertThat(pinStrategy.getMultiplier()).isEqualTo(2.0);
            assertThat(pinStrategy.isProgressiveLockoutEnabled()).isTrue();
            assertThat(pinStrategy.getLockoutDurationAfterExhaustion()).isEqualTo(Duration.ofMinutes(15));
        }

        @Test
        @DisplayName("Should use exponential backoff for SSN")
        void shouldUseExponentialBackoffForSSN() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            TokenRetryStrategy ssnStrategy = strategies.get("SSN");
            assertThat(ssnStrategy.getRetryType()).isEqualTo(TokenRetryStrategy.RetryType.EXPONENTIAL_BACKOFF);
            assertThat(ssnStrategy.getMaxRetries()).isEqualTo(2);
            assertThat(ssnStrategy.getBaseDelayMs()).isEqualTo(3000);
            assertThat(ssnStrategy.getMaxDelayMs()).isEqualTo(30000);
            assertThat(ssnStrategy.isProgressiveLockoutEnabled()).isTrue();
            assertThat(ssnStrategy.getLockoutDurationAfterExhaustion()).isEqualTo(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("Should use linear backoff for voice biometric")
        void shouldUseLinearBackoffForVoiceBiometric() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            TokenRetryStrategy voiceStrategy = strategies.get("VOICE_BIOMETRIC");
            assertThat(voiceStrategy.getRetryType()).isEqualTo(TokenRetryStrategy.RetryType.LINEAR_BACKOFF);
            assertThat(voiceStrategy.getMaxRetries()).isEqualTo(2);
            assertThat(voiceStrategy.getBaseDelayMs()).isEqualTo(10000); // Longer delay for biometric
            assertThat(voiceStrategy.isProgressiveLockoutEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Global Retry Policy Tests")
    class GlobalRetryPolicyTests {

        @Test
        @DisplayName("Should return premium bank global retry policy")
        void shouldReturnPremiumBankGlobalRetryPolicy() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getBrandCode()).isEqualTo("PREMIUM_BANK");
        }

        @Test
        @DisplayName("Should have strict global attempt limits")
        void shouldHaveStrictGlobalAttemptLimits() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getMaxGlobalAttempts()).isEqualTo(5);
            assertThat(globalPolicy.getGlobalLockoutThreshold()).isEqualTo(4);
            assertThat(globalPolicy.getSuspiciousActivityThreshold()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should have longer lockout durations")
        void shouldHaveLongerLockoutDurations() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getGlobalLockoutDuration()).isEqualTo(Duration.ofMinutes(20));
            assertThat(globalPolicy.getSuspiciousActivityLockoutDuration()).isEqualTo(Duration.ofMinutes(30));
            assertThat(globalPolicy.getRetryWindowResetDuration()).isEqualTo(Duration.ofHours(2));
        }

        @Test
        @DisplayName("Should have progressive delay escalation policy")
        void shouldHaveProgressiveDelayEscalationPolicy() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getEscalationPolicy()).isEqualTo(BrandGlobalRetryPolicy.EscalationPolicy.PROGRESSIVE_DELAY);
            assertThat(globalPolicy.getEscalationThreshold()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should enable cross-token delays")
        void shouldEnableCrossTokenDelays() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.isCrossTokenDelayEnabled()).isTrue();
            assertThat(globalPolicy.getCrossTokenDelayMultiplier()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should enable retry analytics")
        void shouldEnableRetryAnalytics() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.isEnableRetryAnalytics()).isTrue();
        }

        @Test
        @DisplayName("Should enable global lockout")
        void shouldEnableGlobalLockout() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.isGlobalLockoutEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Security Comparison Tests")
    class SecurityComparisonTests {

        @Test
        @DisplayName("Should be more restrictive than default configuration")
        void shouldBeMoreRestrictiveThanDefaultConfiguration() {
            // When
            int maxAttempts = config.getMaxOverallAttempts();
            Map<String, Integer> tokenAttempts = config.getBrandSpecificTokenAttempts();
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(maxAttempts).isLessThanOrEqualTo(3); // Strict overall attempts
            assertThat(tokenAttempts.get("SSN")).isEqualTo(2); // Stricter than typical 3
            assertThat(globalPolicy.getGlobalLockoutThreshold()).isLessThanOrEqualTo(4); // Low threshold
            assertThat(globalPolicy.getSuspiciousActivityThreshold()).isLessThanOrEqualTo(4); // Low threshold
        }

        @Test
        @DisplayName("Should use advanced retry strategies")
        void shouldUseAdvancedRetryStrategies() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            boolean hasExponentialBackoff = false;
            for (TokenRetryStrategy strategy : strategies.values()) {
                if (strategy.getRetryType() == TokenRetryStrategy.RetryType.EXPONENTIAL_BACKOFF) {
                    hasExponentialBackoff = true;
                    break;
                }
            }
            
            boolean hasProgressiveLockouts = true;
            for (TokenRetryStrategy strategy : strategies.values()) {
                if (!strategy.isProgressiveLockoutEnabled()) {
                    hasProgressiveLockouts = false;
                    break;
                }
            }

            assertThat(hasExponentialBackoff).isTrue();
            assertThat(hasProgressiveLockouts).isTrue();
        }
    }

    @Nested
    @DisplayName("Configuration Consistency Tests")
    class ConfigurationConsistencyTests {

        @Test
        @DisplayName("Brand specific attempts should exist in token definitions")
        void brandSpecificAttemptsShouldExistInTokenDefinitions() {
            // When
            Map<String, Integer> brandAttempts = config.getBrandSpecificTokenAttempts();
            List<String> definedTokens = new ArrayList<String>();
            for (AuthTokenDefinition token : config.getTokenDefinitions()) {
                definedTokens.add(token.getName());
            }

            // Then
            assertThat(definedTokens).containsAll(brandAttempts.keySet());
        }

        @Test
        @DisplayName("Retry strategy tokens should exist in token definitions")
        void retryStrategyTokensShouldExistInTokenDefinitions() {
            // When
            Map<String, TokenRetryStrategy> retryStrategies = config.getTokenRetryStrategies();
            List<String> definedTokens = new ArrayList<String>();
            for (AuthTokenDefinition token : config.getTokenDefinitions()) {
                definedTokens.add(token.getName());
            }

            // Then
            assertThat(definedTokens).containsAll(retryStrategies.keySet());
        }

        @Test
        @DisplayName("Token max attempts should be consistent")
        void tokenMaxAttemptsShouldBeConsistent() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();
            Map<String, Integer> brandAttempts = config.getBrandSpecificTokenAttempts();

            // Then
            for (AuthTokenDefinition token : tokenDefinitions) {
                String tokenName = token.getName();
                if (brandAttempts.containsKey(tokenName)) {
                    assertThat(token.getMaxAttempts())
                            .as("Max attempts for token %s should be consistent", tokenName)
                            .isEqualTo(brandAttempts.get(tokenName));
                }
            }
        }

        @Test
        @DisplayName("All retry strategies should have progressive lockout enabled")
        void allRetryStrategiesShouldHaveProgressiveLockoutEnabled() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            for (TokenRetryStrategy strategy : strategies.values()) {
                assertThat(strategy.isProgressiveLockoutEnabled())
                        .as("Token %s should have progressive lockout enabled", strategy.getTokenName())
                        .isTrue();
            }
        }
    }
} 