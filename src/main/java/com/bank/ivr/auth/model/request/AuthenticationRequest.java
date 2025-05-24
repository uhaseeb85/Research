package com.bank.ivr.auth.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AuthenticationRequest {
    
    @NotBlank(message = "Session ID is required")
    private final String sessionId;
    
    @NotNull(message = "Customer identifier is required")
    @Valid
    private final CustomerIdentifier customerIdentifier;
    
    private final String attemptId;
    
    @Valid
    private final List<ProvidedToken> providedTokens;
    
    @JsonCreator
    public AuthenticationRequest(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("customerIdentifier") CustomerIdentifier customerIdentifier,
            @JsonProperty("attemptId") String attemptId,
            @JsonProperty("providedTokens") List<ProvidedToken> providedTokens) {
        this.sessionId = sessionId;
        this.customerIdentifier = customerIdentifier;
        this.attemptId = attemptId;
        this.providedTokens = providedTokens;
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
               '}';
    }
} 