package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines global retry policies that apply across all tokens for a specific brand.
 * This controls overall retry behavior and cross-token lockout policies.
 */
public class BrandGlobalRetryPolicy {
    
    public enum EscalationPolicy {
        NONE,              // No escalation, standard retry behavior
        PROGRESSIVE_DELAY, // Increasing delays across all tokens after failures
        BRAND_LOCKOUT,     // Lock out all authentication for this brand after failures
        TOKEN_BLACKLIST    // Disable specific tokens after repeated failures
    }
    
    @JsonProperty("brandCode")
    private final String brandCode;
    
    @JsonProperty("maxGlobalAttempts")
    private final int maxGlobalAttempts;
    
    @JsonProperty("globalLockoutEnabled")
    private final boolean globalLockoutEnabled;
    
    @JsonProperty("globalLockoutThreshold")
    private final int globalLockoutThreshold;
    
    @JsonProperty("globalLockoutDuration")
    private final Duration globalLockoutDuration;
    
    @JsonProperty("escalationPolicy")
    private final EscalationPolicy escalationPolicy;
    
    @JsonProperty("escalationThreshold")
    private final int escalationThreshold;
    
    @JsonProperty("crossTokenDelayEnabled")
    private final boolean crossTokenDelayEnabled;
    
    @JsonProperty("crossTokenDelayMultiplier")
    private final double crossTokenDelayMultiplier;
    
    @JsonProperty("suspiciousActivityThreshold")
    private final int suspiciousActivityThreshold;
    
    @JsonProperty("suspiciousActivityLockoutDuration")
    private final Duration suspiciousActivityLockoutDuration;
    
    @JsonProperty("retryWindowResetDuration")
    private final Duration retryWindowResetDuration;
    
    @JsonProperty("enableRetryAnalytics")
    private final boolean enableRetryAnalytics;
    
    @JsonCreator
    public BrandGlobalRetryPolicy(
            @JsonProperty("brandCode") String brandCode,
            @JsonProperty("maxGlobalAttempts") int maxGlobalAttempts,
            @JsonProperty("globalLockoutEnabled") boolean globalLockoutEnabled,
            @JsonProperty("globalLockoutThreshold") int globalLockoutThreshold,
            @JsonProperty("globalLockoutDuration") Duration globalLockoutDuration,
            @JsonProperty("escalationPolicy") EscalationPolicy escalationPolicy,
            @JsonProperty("escalationThreshold") int escalationThreshold,
            @JsonProperty("crossTokenDelayEnabled") boolean crossTokenDelayEnabled,
            @JsonProperty("crossTokenDelayMultiplier") double crossTokenDelayMultiplier,
            @JsonProperty("suspiciousActivityThreshold") int suspiciousActivityThreshold,
            @JsonProperty("suspiciousActivityLockoutDuration") Duration suspiciousActivityLockoutDuration,
            @JsonProperty("retryWindowResetDuration") Duration retryWindowResetDuration,
            @JsonProperty("enableRetryAnalytics") boolean enableRetryAnalytics) {
        this.brandCode = brandCode;
        this.maxGlobalAttempts = maxGlobalAttempts;
        this.globalLockoutEnabled = globalLockoutEnabled;
        this.globalLockoutThreshold = globalLockoutThreshold;
        this.globalLockoutDuration = globalLockoutDuration;
        this.escalationPolicy = escalationPolicy != null ? escalationPolicy : EscalationPolicy.NONE;
        this.escalationThreshold = escalationThreshold;
        this.crossTokenDelayEnabled = crossTokenDelayEnabled;
        this.crossTokenDelayMultiplier = crossTokenDelayMultiplier;
        this.suspiciousActivityThreshold = suspiciousActivityThreshold;
        this.suspiciousActivityLockoutDuration = suspiciousActivityLockoutDuration;
        this.retryWindowResetDuration = retryWindowResetDuration;
        this.enableRetryAnalytics = enableRetryAnalytics;
    }
    
    // Builder constructor
    private BrandGlobalRetryPolicy(Builder builder) {
        this.brandCode = builder.brandCode;
        this.maxGlobalAttempts = builder.maxGlobalAttempts;
        this.globalLockoutEnabled = builder.globalLockoutEnabled;
        this.globalLockoutThreshold = builder.globalLockoutThreshold;
        this.globalLockoutDuration = builder.globalLockoutDuration;
        this.escalationPolicy = builder.escalationPolicy;
        this.escalationThreshold = builder.escalationThreshold;
        this.crossTokenDelayEnabled = builder.crossTokenDelayEnabled;
        this.crossTokenDelayMultiplier = builder.crossTokenDelayMultiplier;
        this.suspiciousActivityThreshold = builder.suspiciousActivityThreshold;
        this.suspiciousActivityLockoutDuration = builder.suspiciousActivityLockoutDuration;
        this.retryWindowResetDuration = builder.retryWindowResetDuration;
        this.enableRetryAnalytics = builder.enableRetryAnalytics;
    }
    
    // Getters
    public String getBrandCode() {
        return brandCode;
    }
    
    public int getMaxGlobalAttempts() {
        return maxGlobalAttempts;
    }
    
    public boolean isGlobalLockoutEnabled() {
        return globalLockoutEnabled;
    }
    
    public int getGlobalLockoutThreshold() {
        return globalLockoutThreshold;
    }
    
    public Duration getGlobalLockoutDuration() {
        return globalLockoutDuration;
    }
    
    public EscalationPolicy getEscalationPolicy() {
        return escalationPolicy;
    }
    
    public int getEscalationThreshold() {
        return escalationThreshold;
    }
    
    public boolean isCrossTokenDelayEnabled() {
        return crossTokenDelayEnabled;
    }
    
    public double getCrossTokenDelayMultiplier() {
        return crossTokenDelayMultiplier;
    }
    
    public int getSuspiciousActivityThreshold() {
        return suspiciousActivityThreshold;
    }
    
    public Duration getSuspiciousActivityLockoutDuration() {
        return suspiciousActivityLockoutDuration;
    }
    
    public Duration getRetryWindowResetDuration() {
        return retryWindowResetDuration;
    }
    
    public boolean isEnableRetryAnalytics() {
        return enableRetryAnalytics;
    }
    
    /**
     * Determines if escalation should be triggered based on current failures.
     * 
     * @param totalFailures the total number of failures across all tokens
     * @return true if escalation should be triggered
     */
    public boolean shouldTriggerEscalation(int totalFailures) {
        return escalationPolicy != EscalationPolicy.NONE && totalFailures >= escalationThreshold;
    }
    
    /**
     * Determines if global lockout should be triggered.
     * 
     * @param totalFailures the total number of failures
     * @return true if global lockout should be triggered
     */
    public boolean shouldTriggerGlobalLockout(int totalFailures) {
        return globalLockoutEnabled && totalFailures >= globalLockoutThreshold;
    }
    
    /**
     * Determines if suspicious activity lockout should be triggered.
     * 
     * @param rapidFailures the number of rapid successive failures
     * @return true if suspicious activity lockout should be triggered
     */
    public boolean shouldTriggerSuspiciousActivityLockout(int rapidFailures) {
        return rapidFailures >= suspiciousActivityThreshold;
    }
    
    /**
     * Calculates cross-token delay multiplier based on previous failures.
     * 
     * @param previousFailures number of previous failures across tokens
     * @return delay multiplier to apply
     */
    public double calculateCrossTokenDelayMultiplier(int previousFailures) {
        if (!crossTokenDelayEnabled || previousFailures <= 0) {
            return 1.0;
        }
        return Math.pow(crossTokenDelayMultiplier, previousFailures);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String brandCode;
        private int maxGlobalAttempts = 10;
        private boolean globalLockoutEnabled = true;
        private int globalLockoutThreshold = 8;
        private Duration globalLockoutDuration = Duration.ofMinutes(15);
        private EscalationPolicy escalationPolicy = EscalationPolicy.PROGRESSIVE_DELAY;
        private int escalationThreshold = 5;
        private boolean crossTokenDelayEnabled = true;
        private double crossTokenDelayMultiplier = 1.5;
        private int suspiciousActivityThreshold = 6;
        private Duration suspiciousActivityLockoutDuration = Duration.ofMinutes(10);
        private Duration retryWindowResetDuration = Duration.ofHours(2);
        private boolean enableRetryAnalytics = true;
        
        public Builder brandCode(String brandCode) {
            this.brandCode = brandCode;
            return this;
        }
        
        public Builder maxGlobalAttempts(int maxGlobalAttempts) {
            this.maxGlobalAttempts = maxGlobalAttempts;
            return this;
        }
        
        public Builder globalLockoutEnabled(boolean globalLockoutEnabled) {
            this.globalLockoutEnabled = globalLockoutEnabled;
            return this;
        }
        
        public Builder globalLockoutThreshold(int globalLockoutThreshold) {
            this.globalLockoutThreshold = globalLockoutThreshold;
            return this;
        }
        
        public Builder globalLockoutDuration(Duration globalLockoutDuration) {
            this.globalLockoutDuration = globalLockoutDuration;
            return this;
        }
        
        public Builder escalationPolicy(EscalationPolicy escalationPolicy) {
            this.escalationPolicy = escalationPolicy;
            return this;
        }
        
        public Builder escalationThreshold(int escalationThreshold) {
            this.escalationThreshold = escalationThreshold;
            return this;
        }
        
        public Builder crossTokenDelayEnabled(boolean crossTokenDelayEnabled) {
            this.crossTokenDelayEnabled = crossTokenDelayEnabled;
            return this;
        }
        
        public Builder crossTokenDelayMultiplier(double crossTokenDelayMultiplier) {
            this.crossTokenDelayMultiplier = crossTokenDelayMultiplier;
            return this;
        }
        
        public Builder suspiciousActivityThreshold(int suspiciousActivityThreshold) {
            this.suspiciousActivityThreshold = suspiciousActivityThreshold;
            return this;
        }
        
        public Builder suspiciousActivityLockoutDuration(Duration suspiciousActivityLockoutDuration) {
            this.suspiciousActivityLockoutDuration = suspiciousActivityLockoutDuration;
            return this;
        }
        
        public Builder retryWindowResetDuration(Duration retryWindowResetDuration) {
            this.retryWindowResetDuration = retryWindowResetDuration;
            return this;
        }
        
        public Builder enableRetryAnalytics(boolean enableRetryAnalytics) {
            this.enableRetryAnalytics = enableRetryAnalytics;
            return this;
        }
        
        public BrandGlobalRetryPolicy build() {
            Objects.requireNonNull(brandCode, "Brand code is required");
            return new BrandGlobalRetryPolicy(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrandGlobalRetryPolicy that = (BrandGlobalRetryPolicy) o;
        return Objects.equals(brandCode, that.brandCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(brandCode);
    }
    
    @Override
    public String toString() {
        return "BrandGlobalRetryPolicy{" +
                "brandCode='" + brandCode + '\'' +
                ", maxGlobalAttempts=" + maxGlobalAttempts +
                ", globalLockoutEnabled=" + globalLockoutEnabled +
                ", escalationPolicy=" + escalationPolicy +
                ", escalationThreshold=" + escalationThreshold +
                '}';
    }
} 