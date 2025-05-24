package com.bank.ivr.auth.config;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for defining authentication tokens.
 * This allows for easy addition and modification of token definitions without code changes.
 */
@Configuration
public class AuthTokenConfig {
    
    /**
     * Defines the SSN token configuration.
     */
    @Bean
    public AuthTokenDefinition ssnTokenDefinition() {
        return AuthTokenDefinition.builder()
                .name("SSN")
                .description("Social Security Number")
                .priority(100)
                .maskingRegex("\\d{3}-\\d{2}-(\\d{4})")
                .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                .maxAttempts(3)
                .build();
    }
    
    /**
     * Defines the Debit Card PIN token configuration.
     */
    @Bean
    public AuthTokenDefinition debitCardPinTokenDefinition() {
        return AuthTokenDefinition.builder()
                .name("DEBIT_CARD_PIN")
                .description("Debit Card PIN")
                .priority(90)
                .maskingRegex("\\d{4}")
                .inputFormatRegex("^\\d{4}$")
                .maxAttempts(3)
                .build();
    }
    
    /**
     * Defines the Date of Birth token configuration.
     */
    @Bean
    public AuthTokenDefinition dateOfBirthTokenDefinition() {
        return AuthTokenDefinition.builder()
                .name("DATE_OF_BIRTH")
                .description("Date of Birth")
                .priority(80)
                .maskingRegex("(\\d{2})/(\\d{2})/(\\d{4})")
                .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                .maxAttempts(3)
                .build();
    }
    
    /**
     * Defines the Mother's Maiden Name token configuration.
     */
    @Bean
    public AuthTokenDefinition motherMaidenNameTokenDefinition() {
        return AuthTokenDefinition.builder()
                .name("MOTHER_MAIDEN_NAME")
                .description("Mother's Maiden Name")
                .priority(70)
                .maskingRegex("(\\w+)")
                .inputFormatRegex("^[a-zA-Z\\s'-]{2,50}$")
                .maxAttempts(3)
                .build();
    }
    
    /**
     * Defines the Employee ID token configuration.
     */
    @Bean
    public AuthTokenDefinition employeeIdTokenDefinition() {
        return AuthTokenDefinition.builder()
                .name("EMPLOYEE_ID")
                .description("Employee ID")
                .priority(60)
                .maskingRegex("(\\w+)")
                .inputFormatRegex("^[a-zA-Z0-9]{3,20}$")
                .maxAttempts(3)
                .build();
    }
    
    /**
     * Provides a list of all available token definitions.
     * This can be injected into services that need to work with all tokens.
     */
    @Bean
    public List<AuthTokenDefinition> allTokenDefinitions() {
        return Arrays.asList(
                ssnTokenDefinition(),
                debitCardPinTokenDefinition(),
                dateOfBirthTokenDefinition(),
                motherMaidenNameTokenDefinition(),
                employeeIdTokenDefinition()
        );
    }
} 