package com.bank.ivr.auth.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

/**
 * Captures trust level and phone matching information for advanced authentication rules.
 * Used by brands like Royal Bank that require trust-based authentication flows.
 */
public class TrustLevelInfo {
    
    public enum TrustLevel {
        RED, GREEN
    }
    
    public enum PhoneMatchStatus {
        NOT_MATCHED,           // Phone not matched with any SSN
        SINGLE_MATCH,          // Phone matched with exactly one SSN
        MULTIPLE_MATCHES       // Phone matched with multiple SSNs
    }
    
    @NotNull(message = "Trust level is required")
    private final TrustLevel trustLevel;
    
    @NotNull(message = "Phone match status is required")
    private final PhoneMatchStatus phoneMatchStatus;
    
    private final Integer matchedSsnCount;
    
    @JsonCreator
    public TrustLevelInfo(
            @JsonProperty("trustLevel") TrustLevel trustLevel,
            @JsonProperty("phoneMatchStatus") PhoneMatchStatus phoneMatchStatus,
            @JsonProperty("matchedSsnCount") Integer matchedSsnCount) {
        this.trustLevel = trustLevel;
        this.phoneMatchStatus = phoneMatchStatus;
        this.matchedSsnCount = matchedSsnCount;
    }
    
    public TrustLevel getTrustLevel() {
        return trustLevel;
    }
    
    public PhoneMatchStatus getPhoneMatchStatus() {
        return phoneMatchStatus;
    }
    
    public Integer getMatchedSsnCount() {
        return matchedSsnCount;
    }
    
    public boolean isHighTrust() {
        return TrustLevel.GREEN.equals(trustLevel);
    }
    
    public boolean isLowTrust() {
        return TrustLevel.RED.equals(trustLevel);
    }
    
    public boolean hasPhoneMatch() {
        return !PhoneMatchStatus.NOT_MATCHED.equals(phoneMatchStatus);
    }
    
    public boolean hasMultiplePhoneMatches() {
        return PhoneMatchStatus.MULTIPLE_MATCHES.equals(phoneMatchStatus);
    }
    
    public boolean hasSinglePhoneMatch() {
        return PhoneMatchStatus.SINGLE_MATCH.equals(phoneMatchStatus);
    }
    
    @Override
    public String toString() {
        return "TrustLevelInfo{" +
               "trustLevel=" + trustLevel +
               ", phoneMatchStatus=" + phoneMatchStatus +
               ", matchedSsnCount=" + matchedSsnCount +
               '}';
    }
} 