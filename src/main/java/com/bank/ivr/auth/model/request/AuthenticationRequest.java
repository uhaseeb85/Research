package com.bank.ivr.auth.model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthenticationRequest {
    
    @NotBlank(message = "Session ID is required")
    private final String sessionId;
    
    @NotNull(message = "Customer identifier is required")
    @Valid
    private final CustomerIdentifier customerIdentifier;
    
    private final String attemptId;
    
    @Valid
    private final List<ProvidedToken> providedTokens;
    
    @NotBlank(message = "Brand is required")
    private final String brand;
    
    @Valid
    private final TrustLevelInfo trustLevelInfo;
    
    @JsonCreator
    public AuthenticationRequest(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("customerIdentifier") CustomerIdentifier customerIdentifier,
            @JsonProperty("attemptId") String attemptId,
            @JsonProperty("providedTokens") List<ProvidedToken> providedTokens,
            @JsonProperty("brand") String brand,
            @JsonProperty("trustLevelInfo") TrustLevelInfo trustLevelInfo) {
        this.sessionId = sessionId;
        this.customerIdentifier = customerIdentifier;
        this.attemptId = attemptId;
        this.providedTokens = providedTokens;
        this.brand = brand;
        this.trustLevelInfo = trustLevelInfo;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public CustomerIdentifier getCustomerIdentifier() {
        return customerIdentifier;
    }
    
    public String getAttemptId() {
        return attemptId;
    }
    
    public List<ProvidedToken> getProvidedTokens() {
        return providedTokens;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public TrustLevelInfo getTrustLevelInfo() {
        return trustLevelInfo;
    }
    
    public boolean isNewAttempt() {
        return attemptId == null || attemptId.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "AuthenticationRequest{" +
               "sessionId='" + sessionId + '\'' +
               ", customerIdentifier=" + customerIdentifier +
               ", attemptId='" + attemptId + '\'' +
               ", providedTokens=" + providedTokens +
               ", brand='" + brand + '\'' +
               ", trustLevelInfo=" + trustLevelInfo +
               '}';
    }
} 