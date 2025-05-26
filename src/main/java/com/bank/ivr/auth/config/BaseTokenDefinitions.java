package com.bank.ivr.auth.config;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Base token definitions that can be referenced and customized by brand configurations.
 * This eliminates redundancy by providing standard token definitions that brands can
 * override specific properties for (like priority or max attempts) rather than
 * redefining the entire token definition.
 */
@Component
public class BaseTokenDefinitions {
    
    private final Map<String, AuthTokenDefinition> baseTokens;
    
    public BaseTokenDefinitions() {
        this.baseTokens = createBaseTokenDefinitions();
    }
    
    /**
     * Gets a base token definition that can be customized by brands.
     * 
     * @param tokenName the name of the token
     * @return the base token definition, or null if not found
     */
    public AuthTokenDefinition getBaseToken(String tokenName) {
        return baseTokens.get(tokenName);
    }
    
    /**
     * Creates a customized token definition based on the base definition.
     * Only the specified properties are overridden.
     * 
     * @param tokenName the base token name
     * @param customizations map of property names to new values
     * @return customized token definition
     */
    public AuthTokenDefinition createCustomizedToken(String tokenName, Map<String, Object> customizations) {
        AuthTokenDefinition baseToken = baseTokens.get(tokenName);
        if (baseToken == null) {
            throw new IllegalArgumentException("Base token not found: " + tokenName);
        }
        
        // Create a new builder and copy all values from the base token
        AuthTokenDefinition.Builder builder = AuthTokenDefinition.builder()
                .name(baseToken.getName())
                .description(baseToken.getDescription())
                .priority(baseToken.getPriority())
                .inputFormatRegex(baseToken.getInputFormatRegex())
                .maxAttempts(baseToken.getMaxAttempts());
        
        // Apply customizations
        customizations.forEach((property, value) -> {
            switch (property.toLowerCase()) {
                case "priority":
                    builder.priority((Integer) value);
                    break;
                case "maxattempts":
                    builder.maxAttempts((Integer) value);
                    break;
                case "description":
                    builder.description((String) value);
                    break;
                case "inputformatregex":
                    builder.inputFormatRegex((String) value);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown property: " + property);
            }
        });
        
        return builder.build();
    }
    
    /**
     * Gets all available base token names.
     * 
     * @return set of base token names
     */
    public java.util.Set<String> getAvailableTokenNames() {
        return baseTokens.keySet();
    }
    
    /**
     * Creates the standard base token definitions.
     */
    private Map<String, AuthTokenDefinition> createBaseTokenDefinitions() {
        Map<String, AuthTokenDefinition> tokens = new HashMap<>();
        
        // Social Security Number
        tokens.put("SSN", AuthTokenDefinition.builder()
                .name("SSN")
                .description("Social Security Number")
                .priority(90) // Default priority
                .inputFormatRegex("^\\d{9}$|^\\d{3}-\\d{2}-\\d{4}$")
                .maxAttempts(3) // Default attempts
                .build());
        
        // Date of Birth
        tokens.put("DATE_OF_BIRTH", AuthTokenDefinition.builder()
                .name("DATE_OF_BIRTH")
                .description("Date of Birth")
                .priority(85)
                .inputFormatRegex("^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$")
                .maxAttempts(3)
                .build());
        
        // Debit Card PIN
        tokens.put("DEBIT_CARD_PIN", AuthTokenDefinition.builder()
                .name("DEBIT_CARD_PIN")
                .description("Debit Card PIN")
                .priority(80)
                .inputFormatRegex("^\\d{4}$")
                .maxAttempts(3)
                .build());
        
        // Mother's Maiden Name
        tokens.put("MOTHER_MAIDEN_NAME", AuthTokenDefinition.builder()
                .name("MOTHER_MAIDEN_NAME")
                .description("Mother's Maiden Name")
                .priority(75)
                .inputFormatRegex("^[a-zA-Z\\s'-]{2,50}$")
                .maxAttempts(3)
                .build());
        
        // Account Opening Date
        tokens.put("ACCOUNT_OPENING_DATE", AuthTokenDefinition.builder()
                .name("ACCOUNT_OPENING_DATE")
                .description("Account Opening Date")
                .priority(70)
                .inputFormatRegex("^\\d{2}/\\d{4}$")
                .maxAttempts(3)
                .build());
        
        // Voice Biometric (Premium feature)
        tokens.put("VOICE_BIOMETRIC", AuthTokenDefinition.builder()
                .name("VOICE_BIOMETRIC")
                .description("Voice Authentication")
                .priority(65)
                .inputFormatRegex("^VOICE_MATCH$")
                .maxAttempts(2)
                .build());
        
        // Mobile PIN (Tech bank feature)
        tokens.put("MOBILE_PIN", AuthTokenDefinition.builder()
                .name("MOBILE_PIN")
                .description("Mobile Banking PIN")
                .priority(95)
                .inputFormatRegex("^\\d{4,6}$")
                .maxAttempts(3)
                .build());
        
        // Biometric ID (Tech bank feature)
        tokens.put("BIOMETRIC_ID", AuthTokenDefinition.builder()
                .name("BIOMETRIC_ID")
                .description("Biometric Authentication")
                .priority(90)
                .inputFormatRegex("^BIO_[A-Z0-9]{8,}$")
                .maxAttempts(2)
                .build());
        
        // Account Number
        tokens.put("ACCOUNT_NUMBER", AuthTokenDefinition.builder()
                .name("ACCOUNT_NUMBER")
                .description("Account Number")
                .priority(60)
                .inputFormatRegex("^\\d{8,16}$")
                .maxAttempts(3)
                .build());
        
        // Security Question
        tokens.put("SECURITY_QUESTION", AuthTokenDefinition.builder()
                .name("SECURITY_QUESTION")
                .description("Security Question Answer")
                .priority(55)
                .inputFormatRegex("^.{3,50}$")
                .maxAttempts(2)
                .build());
        
        return tokens;
    }
} 