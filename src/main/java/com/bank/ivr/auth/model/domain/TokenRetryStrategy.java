package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines retry strategy configuration for authentication tokens.
 * Supports various retry patterns including immediate retry, fixed delay, and exponential backoff.
 */
public class TokenRetryStrategy {
    
    public enum RetryType {
        IMMEDIATE,          // No delay between retries
        FIXED_DELAY,        // Fixed delay between retries
        EXPONENTIAL_BACKOFF, // Increasing delay between retries
        LINEAR_BACKOFF      // Linear increase in delay
    }
    
    @JsonProperty("tokenName")
    private final String tokenName;
    
    @JsonProperty("retryType")
    private final RetryType retryType;
    
    @JsonProperty("maxRetries")
    private final int maxRetries;
    
    @JsonProperty("baseDelayMs")
    private final long baseDelayMs;
    
    @JsonProperty("maxDelayMs")
    private final long maxDelayMs;
    
    @JsonProperty("multiplier")
    private final double multiplier;
    
    @JsonProperty("progressiveLockoutEnabled")
    private final boolean progressiveLockoutEnabled;
    
    @JsonProperty("lockoutDurationAfterExhaustion")
    private final Duration lockoutDurationAfterExhaustion;
    
    @JsonProperty("resetWindowDuration")
    private final Duration resetWindowDuration;
    
    @JsonCreator
    public TokenRetryStrategy(
            @JsonProperty("tokenName") String tokenName,
            @JsonProperty("retryType") RetryType retryType,
            @JsonProperty("maxRetries") int maxRetries,
            @JsonProperty("baseDelayMs") long baseDelayMs,
            @JsonProperty("maxDelayMs") long maxDelayMs,
            @JsonProperty("multiplier") double multiplier,
            @JsonProperty("progressiveLockoutEnabled") boolean progressiveLockoutEnabled,
            @JsonProperty("lockoutDurationAfterExhaustion") Duration lockoutDurationAfterExhaustion,
            @JsonProperty("resetWindowDuration") Duration resetWindowDuration) {
        this.tokenName = tokenName;
        this.retryType = retryType;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
        this.progressiveLockoutEnabled = progressiveLockoutEnabled;
        this.lockoutDurationAfterExhaustion = lockoutDurationAfterExhaustion;
        this.resetWindowDuration = resetWindowDuration;
    }
    
    // Builder constructor
    private TokenRetryStrategy(Builder builder) {
        this.tokenName = builder.tokenName;
        this.retryType = builder.retryType;
        this.maxRetries = builder.maxRetries;
        this.baseDelayMs = builder.baseDelayMs;
        this.maxDelayMs = builder.maxDelayMs;
        this.multiplier = builder.multiplier;
        this.progressiveLockoutEnabled = builder.progressiveLockoutEnabled;
        this.lockoutDurationAfterExhaustion = builder.lockoutDurationAfterExhaustion;
        this.resetWindowDuration = builder.resetWindowDuration;
    }
    
    // Getters
    public String getTokenName() {
        return tokenName;
    }
    
    public RetryType getRetryType() {
        return retryType;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public long getBaseDelayMs() {
        return baseDelayMs;
    }
    
    public long getMaxDelayMs() {
        return maxDelayMs;
    }
    
    public double getMultiplier() {
        return multiplier;
    }
    
    public boolean isProgressiveLockoutEnabled() {
        return progressiveLockoutEnabled;
    }
    
    public Duration getLockoutDurationAfterExhaustion() {
        return lockoutDurationAfterExhaustion;
    }
    
    public Duration getResetWindowDuration() {
        return resetWindowDuration;
    }
    
    /**
     * Calculates the delay before the next retry attempt.
     * 
     * @param attemptNumber the current attempt number (0-based)
     * @return delay in milliseconds
     */
    public long calculateDelayMs(int attemptNumber) {
        if (retryType == RetryType.IMMEDIATE) {
            return 0;
        }
        
        long delay;
        switch (retryType) {
            case FIXED_DELAY:
                delay = baseDelayMs;
                break;
            case EXPONENTIAL_BACKOFF:
                delay = (long) (baseDelayMs * Math.pow(multiplier, attemptNumber));
                break;
            case LINEAR_BACKOFF:
                delay = baseDelayMs + (long) (baseDelayMs * multiplier * attemptNumber);
                break;
            default:
                delay = baseDelayMs;
        }
        
        return Math.min(delay, maxDelayMs);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String tokenName;
        private RetryType retryType = RetryType.IMMEDIATE;
        private int maxRetries = 3;
        private long baseDelayMs = 1000; // 1 second default
        private long maxDelayMs = 30000; // 30 seconds max
        private double multiplier = 2.0; // Double delay each time
        private boolean progressiveLockoutEnabled = false;
        private Duration lockoutDurationAfterExhaustion = Duration.ofMinutes(5);
        private Duration resetWindowDuration = Duration.ofHours(1);
        
        public Builder tokenName(String tokenName) {
            this.tokenName = tokenName;
            return this;
        }
        
        public Builder retryType(RetryType retryType) {
            this.retryType = retryType;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder baseDelayMs(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
            return this;
        }
        
        public Builder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }
        
        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }
        
        public Builder progressiveLockoutEnabled(boolean progressiveLockoutEnabled) {
            this.progressiveLockoutEnabled = progressiveLockoutEnabled;
            return this;
        }
        
        public Builder lockoutDurationAfterExhaustion(Duration lockoutDurationAfterExhaustion) {
            this.lockoutDurationAfterExhaustion = lockoutDurationAfterExhaustion;
            return this;
        }
        
        public Builder resetWindowDuration(Duration resetWindowDuration) {
            this.resetWindowDuration = resetWindowDuration;
            return this;
        }
        
        public TokenRetryStrategy build() {
            Objects.requireNonNull(tokenName, "Token name is required");
            return new TokenRetryStrategy(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenRetryStrategy that = (TokenRetryStrategy) o;
        return Objects.equals(tokenName, that.tokenName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tokenName);
    }
    
    @Override
    public String toString() {
        return "TokenRetryStrategy{" +
                "tokenName='" + tokenName + '\'' +
                ", retryType=" + retryType +
                ", maxRetries=" + maxRetries +
                ", baseDelayMs=" + baseDelayMs +
                '}';
    }
} 