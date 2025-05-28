package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for DNIS (Dialed Number Identification Service) specific authentication rules.
 * Contains boolean flags that determine which features are applicable for a specific DNIS.
 */
public class DnisConfiguration {
    
    private final String dnis;
    private final String description;
    
    // Feature flags for DNIS-specific authentication rules
    private final boolean allowSsnAuthentication;
    private final boolean allowPinAuthentication;
    private final boolean allowDateOfBirthAuthentication;
    private final boolean allowMotherMaidenNameAuthentication;
    private final boolean allowAccountNumberAuthentication;
    private final boolean requireMultiFactorAuth;
    private final boolean allowTrustLevelBypass;
    private final boolean enablePhoneMatchValidation;
    private final boolean allowAlternativeTokens;
    private final boolean enableStrictValidation;
    private final boolean allowRetryOnFailure;
    private final boolean enableAuditLogging;
    
    // Timeout and attempt configurations
    private final int maxAuthenticationAttempts;
    private final int sessionTimeoutMinutes;
    
    @JsonCreator
    public DnisConfiguration(
            @JsonProperty("dnis") String dnis,
            @JsonProperty("description") String description,
            @JsonProperty("allowSsnAuthentication") Boolean allowSsnAuthentication,
            @JsonProperty("allowPinAuthentication") Boolean allowPinAuthentication,
            @JsonProperty("allowDateOfBirthAuthentication") Boolean allowDateOfBirthAuthentication,
            @JsonProperty("allowMotherMaidenNameAuthentication") Boolean allowMotherMaidenNameAuthentication,
            @JsonProperty("allowAccountNumberAuthentication") Boolean allowAccountNumberAuthentication,
            @JsonProperty("requireMultiFactorAuth") Boolean requireMultiFactorAuth,
            @JsonProperty("allowTrustLevelBypass") Boolean allowTrustLevelBypass,
            @JsonProperty("enablePhoneMatchValidation") Boolean enablePhoneMatchValidation,
            @JsonProperty("allowAlternativeTokens") Boolean allowAlternativeTokens,
            @JsonProperty("enableStrictValidation") Boolean enableStrictValidation,
            @JsonProperty("allowRetryOnFailure") Boolean allowRetryOnFailure,
            @JsonProperty("enableAuditLogging") Boolean enableAuditLogging,
            @JsonProperty("maxAuthenticationAttempts") Integer maxAuthenticationAttempts,
            @JsonProperty("sessionTimeoutMinutes") Integer sessionTimeoutMinutes) {
        
        this.dnis = dnis;
        this.description = description;
        this.allowSsnAuthentication = allowSsnAuthentication != null ? allowSsnAuthentication : true;
        this.allowPinAuthentication = allowPinAuthentication != null ? allowPinAuthentication : true;
        this.allowDateOfBirthAuthentication = allowDateOfBirthAuthentication != null ? allowDateOfBirthAuthentication : true;
        this.allowMotherMaidenNameAuthentication = allowMotherMaidenNameAuthentication != null ? allowMotherMaidenNameAuthentication : true;
        this.allowAccountNumberAuthentication = allowAccountNumberAuthentication != null ? allowAccountNumberAuthentication : true;
        this.requireMultiFactorAuth = requireMultiFactorAuth != null ? requireMultiFactorAuth : false;
        this.allowTrustLevelBypass = allowTrustLevelBypass != null ? allowTrustLevelBypass : false;
        this.enablePhoneMatchValidation = enablePhoneMatchValidation != null ? enablePhoneMatchValidation : true;
        this.allowAlternativeTokens = allowAlternativeTokens != null ? allowAlternativeTokens : true;
        this.enableStrictValidation = enableStrictValidation != null ? enableStrictValidation : false;
        this.allowRetryOnFailure = allowRetryOnFailure != null ? allowRetryOnFailure : true;
        this.enableAuditLogging = enableAuditLogging != null ? enableAuditLogging : true;
        this.maxAuthenticationAttempts = maxAuthenticationAttempts != null ? maxAuthenticationAttempts : 3;
        this.sessionTimeoutMinutes = sessionTimeoutMinutes != null ? sessionTimeoutMinutes : 15;
    }
    
    // Getters
    public String getDnis() { return dnis; }
    public String getDescription() { return description; }
    public boolean isAllowSsnAuthentication() { return allowSsnAuthentication; }
    public boolean isAllowPinAuthentication() { return allowPinAuthentication; }
    public boolean isAllowDateOfBirthAuthentication() { return allowDateOfBirthAuthentication; }
    public boolean isAllowMotherMaidenNameAuthentication() { return allowMotherMaidenNameAuthentication; }
    public boolean isAllowAccountNumberAuthentication() { return allowAccountNumberAuthentication; }
    public boolean isRequireMultiFactorAuth() { return requireMultiFactorAuth; }
    public boolean isAllowTrustLevelBypass() { return allowTrustLevelBypass; }
    public boolean isEnablePhoneMatchValidation() { return enablePhoneMatchValidation; }
    public boolean isAllowAlternativeTokens() { return allowAlternativeTokens; }
    public boolean isEnableStrictValidation() { return enableStrictValidation; }
    public boolean isAllowRetryOnFailure() { return allowRetryOnFailure; }
    public boolean isEnableAuditLogging() { return enableAuditLogging; }
    public int getMaxAuthenticationAttempts() { return maxAuthenticationAttempts; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    
    @Override
    public String toString() {
        return "DnisConfiguration{" +
               "dnis='" + dnis + '\'' +
               ", description='" + description + '\'' +
               ", allowSsnAuthentication=" + allowSsnAuthentication +
               ", allowPinAuthentication=" + allowPinAuthentication +
               ", requireMultiFactorAuth=" + requireMultiFactorAuth +
               ", maxAuthenticationAttempts=" + maxAuthenticationAttempts +
               ", sessionTimeoutMinutes=" + sessionTimeoutMinutes +
               '}';
    }
} 