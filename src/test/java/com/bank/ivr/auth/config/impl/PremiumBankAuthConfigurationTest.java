package com.bank.ivr.auth.config.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;

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
        @DisplayName("Should have high priority")
        void shouldHaveHighPriority() {
            // When
            int priority = config.getPriority();

            // Then
            assertThat(priority).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Token Definition Tests")
    class TokenDefinitionTests {

        @Test
        @DisplayName("Should return token definitions in priority order")
        void shouldReturnTokenDefinitionsInPriorityOrder() {
            // When
            List<AuthTokenDefinition> definitions = config.getTokenDefinitions();

            // Then
            assertThat(definitions).hasSize(5);
            
            // Verify priority order (highest first)
            assertThat(definitions.get(0).getName()).isEqualTo("DEBIT_CARD_PIN");
            assertThat(definitions.get(0).getPriority()).isEqualTo(100);
            
            assertThat(definitions.get(1).getName()).isEqualTo("SSN");
            assertThat(definitions.get(1).getPriority()).isEqualTo(95);
            
            assertThat(definitions.get(2).getName()).isEqualTo("DATE_OF_BIRTH");
            assertThat(definitions.get(2).getPriority()).isEqualTo(90);
            
            assertThat(definitions.get(3).getName()).isEqualTo("MOTHER_MAIDEN_NAME");
            assertThat(definitions.get(3).getPriority()).isEqualTo(85);
            
            assertThat(definitions.get(4).getName()).isEqualTo("VOICE_BIOMETRIC");
            assertThat(definitions.get(4).getPriority()).isEqualTo(80);
        }

        @Test
        @DisplayName("Should have correct token names")
        void shouldHaveCorrectTokenNames() {
            // When
            List<AuthTokenDefinition> definitions = config.getTokenDefinitions();

            // Then
            List<String> tokenNames = new ArrayList<>();
            for (AuthTokenDefinition def : definitions) {
                tokenNames.add(def.getName());
            }
            
            assertThat(tokenNames).containsExactly(
                "DEBIT_CARD_PIN", "SSN", "DATE_OF_BIRTH", "MOTHER_MAIDEN_NAME", "VOICE_BIOMETRIC"
            );
        }

        @Test
        @DisplayName("Should have appropriate max attempts for each token")
        void shouldHaveAppropriateMaxAttemptsForEachToken() {
            // When
            List<AuthTokenDefinition> definitions = config.getTokenDefinitions();

            // Then
            for (AuthTokenDefinition def : definitions) {
                switch (def.getName()) {
                    case "DEBIT_CARD_PIN":
                        assertThat(def.getMaxAttempts()).isEqualTo(3);
                        break;
                    case "SSN":
                        assertThat(def.getMaxAttempts()).isEqualTo(2); // Stricter for premium
                        break;
                    case "DATE_OF_BIRTH":
                        assertThat(def.getMaxAttempts()).isEqualTo(3);
                        break;
                    case "MOTHER_MAIDEN_NAME":
                        assertThat(def.getMaxAttempts()).isEqualTo(2);
                        break;
                    case "VOICE_BIOMETRIC":
                        assertThat(def.getMaxAttempts()).isEqualTo(2);
                        break;
                }
            }
        }

        @Test
        @DisplayName("Should have valid input format regex for each token")
        void shouldHaveValidInputFormatRegexForEachToken() {
            // When
            List<AuthTokenDefinition> definitions = config.getTokenDefinitions();

            // Then
            for (AuthTokenDefinition def : definitions) {
                assertThat(def.getInputFormatRegex()).isNotNull();
                assertThat(def.getInputFormatRegex()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("Should have descriptions for all tokens")
        void shouldHaveDescriptionsForAllTokens() {
            // When
            List<AuthTokenDefinition> definitions = config.getTokenDefinitions();

            // Then
            for (AuthTokenDefinition def : definitions) {
                assertThat(def.getDescription()).isNotNull();
                assertThat(def.getDescription()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Attempt Limits Tests")
    class AttemptLimitsTests {

        @Test
        @DisplayName("Should have strict overall attempt limit")
        void shouldHaveStrictOverallAttemptLimit() {
            // When
            int maxAttempts = config.getMaxOverallAttempts();

            // Then
            assertThat(maxAttempts).isEqualTo(3); // Stricter than default
        }

        @Test
        @DisplayName("Should have brand-specific token attempt limits")
        void shouldHaveBrandSpecificTokenAttemptLimits() {
            // When
            Map<String, Integer> attempts = config.getBrandSpecificTokenAttempts();

            // Then
            assertThat(attempts).hasSize(5);
            assertThat(attempts.get("SSN")).isEqualTo(2); // Override to be more restrictive
            assertThat(attempts.get("DEBIT_CARD_PIN")).isEqualTo(3);
            assertThat(attempts.get("DATE_OF_BIRTH")).isEqualTo(3);
            assertThat(attempts.get("MOTHER_MAIDEN_NAME")).isEqualTo(2);
            assertThat(attempts.get("VOICE_BIOMETRIC")).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Authentication Behavior Tests")
    class AuthenticationBehaviorTests {

        @Test
        @DisplayName("Should allow concurrent token authentication")
        void shouldAllowConcurrentTokenAuthentication() {
            // When
            boolean allowsConcurrent = config.isConcurrentTokenAuthAllowed();

            // Then
            assertThat(allowsConcurrent).isTrue(); // Premium customers can provide multiple tokens at once
        }
    }

    @Nested
    @DisplayName("Brand Messages Tests")
    class BrandMessagesTests {

        @Test
        @DisplayName("Should have premium-specific welcome message")
        void shouldHavePremiumSpecificWelcomeMessage() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            assertThat(messages.get("welcome"))
                .isEqualTo("Welcome to Premium Bank. For your security, we require enhanced authentication.");
        }

        @Test
        @DisplayName("Should have premium-specific failure message")
        void shouldHavePremiumSpecificFailureMessage() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            assertThat(messages.get("failure"))
                .isEqualTo("Authentication failed. Please contact Premium Support at 1-800-PREMIUM.");
        }

        @Test
        @DisplayName("Should have all required message keys")
        void shouldHaveAllRequiredMessageKeys() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            assertThat(messages).containsKeys(
                "welcome", "primary_prompt", "secondary_prompt", "success", "failure",
                "customer_not_found", "session_expired", "system_error", "no_methods"
            );
        }

        @Test
        @DisplayName("Should have non-empty messages")
        void shouldHaveNonEmptyMessages() {
            // When
            Map<String, String> messages = config.getBrandMessages();

            // Then
            for (String message : messages.values()) {
                assertThat(message).isNotNull();
                assertThat(message).isNotEmpty();
            }
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
            for (AuthTokenDefinition def : config.getTokenDefinitions()) {
                definedTokens.add(def.getName());
            }

            // Then
            for (String tokenName : brandAttempts.keySet()) {
                assertThat(definedTokens).contains(tokenName);
            }
        }

        @Test
        @DisplayName("Should be consistent with premium security requirements")
        void shouldBeConsistentWithPremiumSecurityRequirements() {
            // When
            int maxOverallAttempts = config.getMaxOverallAttempts();
            Map<String, Integer> tokenAttempts = config.getBrandSpecificTokenAttempts();
            boolean allowsConcurrent = config.isConcurrentTokenAuthAllowed();

            // Then
            assertThat(maxOverallAttempts).isLessThanOrEqualTo(3); // Strict
            assertThat(tokenAttempts.get("SSN")).isLessThanOrEqualTo(2); // Very strict for SSN
            assertThat(allowsConcurrent).isTrue(); // Premium feature
        }
    }
} 