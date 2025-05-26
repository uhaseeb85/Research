package com.bank.ivr.auth.model.domain;

import com.bank.ivr.auth.model.request.CustomerIdentifier;
import com.bank.ivr.auth.model.request.TrustLevelInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Represents the session-level information for an authentication attempt.
 * Extracted from AuthenticationContext to improve separation of concerns.
 */
public class AuthenticationSession {
    
    @JsonProperty("attemptId")
    private final String attemptId;
    
    @JsonProperty("sessionId")
    private final String sessionId;
    
    @JsonProperty("customerIdentifier")
    private final CustomerIdentifier customerIdentifier;
    
    @JsonProperty("brand")
    private final String brand;
    
    @JsonProperty("startTime")
    private final LocalDateTime startTime;
    
    @JsonProperty("trustLevelInfo")
    private final TrustLevelInfo trustLevelInfo;
    
    @JsonCreator
    public AuthenticationSession(
            @JsonProperty("attemptId") String attemptId,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("customerIdentifier") CustomerIdentifier customerIdentifier,
            @JsonProperty("brand") String brand,
            @JsonProperty("startTime") LocalDateTime startTime,
            @JsonProperty("trustLevelInfo") TrustLevelInfo trustLevelInfo) {
        this.attemptId = attemptId;
        this.sessionId = sessionId;
        this.customerIdentifier = customerIdentifier;
        this.brand = brand;
        this.startTime = startTime;
        this.trustLevelInfo = trustLevelInfo;
    }
    
    // Getters
    public String getAttemptId() {
        return attemptId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public CustomerIdentifier getCustomerIdentifier() {
        return customerIdentifier;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public TrustLevelInfo getTrustLevelInfo() {
        return trustLevelInfo;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String attemptId;
        private String sessionId;
        private CustomerIdentifier customerIdentifier;
        private String brand;
        private LocalDateTime startTime;
        private TrustLevelInfo trustLevelInfo;
        
        public Builder attemptId(String attemptId) {
            this.attemptId = attemptId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder customerIdentifier(CustomerIdentifier customerIdentifier) {
            this.customerIdentifier = customerIdentifier;
            return this;
        }
        
        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder trustLevelInfo(TrustLevelInfo trustLevelInfo) {
            this.trustLevelInfo = trustLevelInfo;
            return this;
        }
        
        public AuthenticationSession build() {
            return new AuthenticationSession(attemptId, sessionId, customerIdentifier, brand, startTime, trustLevelInfo);
        }
    }
    
    @Override
    public String toString() {
        return String.format("AuthenticationSession{attemptId='%s', sessionId='%s', brand='%s', startTime=%s}",
                attemptId, sessionId, brand, startTime);
    }
} 