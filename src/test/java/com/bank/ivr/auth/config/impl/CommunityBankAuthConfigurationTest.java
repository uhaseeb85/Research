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

@DisplayName("Community Bank Authentication Configuration Tests")
class CommunityBankAuthConfigurationTest {

    private CommunityBankAuthConfiguration config;

    @BeforeEach
    void setUp() {
        config = new CommunityBankAuthConfiguration();
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
            assertThat(brandCode).isEqualTo("COMMUNITY_BANK");
        }

        @Test
        @DisplayName("Should return correct priority")
        void shouldReturnCorrectPriority() {
            // When
            int priority = config.getPriority();

            // Then
            assertThat(priority).isEqualTo(50);
        }

        @Test
        @DisplayName("Should return correct max overall attempts")
        void shouldReturnCorrectMaxOverallAttempts() {
            // When
            int maxAttempts = config.getMaxOverallAttempts();

            // Then
            assertThat(maxAttempts).isEqualTo(5);
        }

        @Test
        @DisplayName("Should not allow concurrent token authentication")
        void shouldNotAllowConcurrentTokenAuthentication() {
            // When
            boolean concurrentAllowed = config.isConcurrentTokenAuthAllowed();

            // Then
            assertThat(concurrentAllowed).isFalse();
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
                    "SSN", "DATE_OF_BIRTH", "MOTHER_MAIDEN_NAME", 
                    "DEBIT_CARD_PIN", "ACCOUNT_OPENING_DATE"
            );
        }

        @Test
        @DisplayName("Should have SSN as highest priority token")
        void shouldHaveSSNAsHighestPriorityToken() {
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

            assertThat(ssnToken.getPriority()).isEqualTo(100);
            assertThat(ssnToken.getDescription()).isEqualTo("Social Security Number");
            assertThat(ssnToken.getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should have correct date of birth configuration")
        void shouldHaveCorrectDateOfBirthConfiguration() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            AuthTokenDefinition dobToken = null;
            for (AuthTokenDefinition token : tokenDefinitions) {
                if ("DATE_OF_BIRTH".equals(token.getName())) {
                    dobToken = token;
                    break;
                }
            }
            assertThat(dobToken).isNotNull();

            assertThat(dobToken.getPriority()).isEqualTo(95);
            assertThat(dobToken.getDescription()).isEqualTo("Date of Birth");
            assertThat(dobToken.getInputFormatRegex()).isEqualTo("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$");
        }

        @Test
        @DisplayName("Should have community-specific account opening date token")
        void shouldHaveCommunitySpecificAccountOpeningDateToken() {
            // When
            List<AuthTokenDefinition> tokenDefinitions = config.getTokenDefinitions();

            // Then
            AuthTokenDefinition accountOpeningToken = null;
            for (AuthTokenDefinition token : tokenDefinitions) {
                if ("ACCOUNT_OPENING_DATE".equals(token.getName())) {
                    accountOpeningToken = token;
                    break;
                }
            }
            assertThat(accountOpeningToken).isNotNull();

            assertThat(accountOpeningToken.getPriority()).isEqualTo(80);
            assertThat(accountOpeningToken.getDescription()).isEqualTo("Account Opening Date");
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
        @DisplayName("Should return correct token attempts mapping")
        void shouldReturnCorrectTokenAttemptsMapping() {
            // When
            Map<String, Integer> tokenAttempts = config.getBrandSpecificTokenAttempts();

            // Then
            assertThat(tokenAttempts).hasSize(5);
            assertThat(tokenAttempts.get("SSN")).isEqualTo(3);
            assertThat(tokenAttempts.get("DATE_OF_BIRTH")).isEqualTo(3);
            assertThat(tokenAttempts.get("MOTHER_MAIDEN_NAME")).isEqualTo(3);
            assertThat(tokenAttempts.get("DEBIT_CARD_PIN")).isEqualTo(3);
            assertThat(tokenAttempts.get("ACCOUNT_OPENING_DATE")).isEqualTo(3);
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
        @DisplayName("Should have community-friendly welcome message")
        void shouldHaveCommunityFriendlyWelcomeMessage() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            String welcomeMessage = messages.get("welcome");
            assertThat(welcomeMessage).contains("Community Bank");
            assertThat(welcomeMessage).contains("We're here to help");
        }

        @Test
        @DisplayName("Should have helpful failure message")
        void shouldHaveHelpfulFailureMessage() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            String failureMessage = messages.get("failure");
            assertThat(failureMessage).contains("local Community Bank branch");
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
            assertThat(strategies).containsKeys("SSN", "DEBIT_CARD_PIN", "DATE_OF_BIRTH");
        }

        @Test
        @DisplayName("Should use immediate retry for SSN")
        void shouldUseImmediateRetryForSSN() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            TokenRetryStrategy ssnStrategy = strategies.get("SSN");
            assertThat(ssnStrategy.getTokenName()).isEqualTo("SSN");
            assertThat(ssnStrategy.getRetryType()).isEqualTo(TokenRetryStrategy.RetryType.IMMEDIATE);
            assertThat(ssnStrategy.getMaxRetries()).isEqualTo(3);
            assertThat(ssnStrategy.isProgressiveLockoutEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should use fixed delay for debit card PIN")
        void shouldUseFixedDelayForDebitCardPIN() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            TokenRetryStrategy pinStrategy = strategies.get("DEBIT_CARD_PIN");
            assertThat(pinStrategy.getRetryType()).isEqualTo(TokenRetryStrategy.RetryType.FIXED_DELAY);
            assertThat(pinStrategy.getBaseDelayMs()).isEqualTo(2000);
            assertThat(pinStrategy.isProgressiveLockoutEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should use immediate retry for date of birth")
        void shouldUseImmediateRetryForDateOfBirth() {
            // When
            Map<String, TokenRetryStrategy> strategies = config.getTokenRetryStrategies();

            // Then
            TokenRetryStrategy dobStrategy = strategies.get("DATE_OF_BIRTH");
            assertThat(dobStrategy.getRetryType()).isEqualTo(TokenRetryStrategy.RetryType.IMMEDIATE);
            assertThat(dobStrategy.isProgressiveLockoutEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Global Retry Policy Tests")
    class GlobalRetryPolicyTests {

        @Test
        @DisplayName("Should return community bank global retry policy")
        void shouldReturnCommunityBankGlobalRetryPolicy() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getBrandCode()).isEqualTo("COMMUNITY_BANK");
        }

        @Test
        @DisplayName("Should have lenient global attempt limits")
        void shouldHaveLenientGlobalAttemptLimits() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getMaxGlobalAttempts()).isEqualTo(8);
            assertThat(globalPolicy.getGlobalLockoutThreshold()).isEqualTo(10);
            assertThat(globalPolicy.getSuspiciousActivityThreshold()).isEqualTo(12);
        }

        @Test
        @DisplayName("Should have shorter lockout durations")
        void shouldHaveShorterLockoutDurations() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getGlobalLockoutDuration()).isEqualTo(Duration.ofMinutes(5));
            assertThat(globalPolicy.getRetryWindowResetDuration()).isEqualTo(Duration.ofMinutes(30));
        }

        @Test
        @DisplayName("Should have no escalation policy")
        void shouldHaveNoEscalationPolicy() {
            // When
            BrandGlobalRetryPolicy globalPolicy = config.getGlobalRetryPolicy();

            // Then
            assertThat(globalPolicy.getEscalationPolicy()).isEqualTo(BrandGlobalRetryPolicy.EscalationPolicy.NONE);
            assertThat(globalPolicy.isCrossTokenDelayEnabled()).isFalse();
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
    }
} 