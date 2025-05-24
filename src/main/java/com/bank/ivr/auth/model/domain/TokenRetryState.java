package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tracks the retry state for a specific token including attempt history and lockout status.
 */
public class TokenRetryState {
    
    public enum LockoutStatus {
        NONE,           // No lockout active
        RETRY_DELAY,    // Waiting for retry delay to elapse
        LOCKED_OUT,     // Locked out after exhausting retries
        PERMANENTLY_FAILED // Token permanently failed for this session
    }
    
    @JsonProperty("tokenName")
    private final String tokenName;
    
    @JsonProperty("attemptCount")
    private int attemptCount;
    
    @JsonProperty("maxAttempts")
    private int maxAttempts;
    
    @JsonProperty("consecutiveFailures")
    private int consecutiveFailures;
    
    @JsonProperty("lockoutStatus")
    private LockoutStatus lockoutStatus;
    
    @JsonProperty("nextRetryAllowedAt")
    private LocalDateTime nextRetryAllowedAt;
    
    @JsonProperty("lockoutExpiresAt")
    private LocalDateTime lockoutExpiresAt;
    
    @JsonProperty("firstAttemptTime")
    private LocalDateTime firstAttemptTime;
    
    @JsonProperty("lastAttemptTime")
    private LocalDateTime lastAttemptTime;
    
    @JsonProperty("retryHistory")
    private List<RetryAttempt> retryHistory;
    
    @JsonProperty("lockoutWindowResetAt")
    private LocalDateTime lockoutWindowResetAt;
    
    @JsonCreator
    public TokenRetryState(
            @JsonProperty("tokenName") String tokenName,
            @JsonProperty("attemptCount") int attemptCount,
            @JsonProperty("maxAttempts") int maxAttempts,
            @JsonProperty("consecutiveFailures") int consecutiveFailures,
            @JsonProperty("lockoutStatus") LockoutStatus lockoutStatus,
            @JsonProperty("nextRetryAllowedAt") LocalDateTime nextRetryAllowedAt,
            @JsonProperty("lockoutExpiresAt") LocalDateTime lockoutExpiresAt,
            @JsonProperty("firstAttemptTime") LocalDateTime firstAttemptTime,
            @JsonProperty("lastAttemptTime") LocalDateTime lastAttemptTime,
            @JsonProperty("retryHistory") List<RetryAttempt> retryHistory,
            @JsonProperty("lockoutWindowResetAt") LocalDateTime lockoutWindowResetAt) {
        this.tokenName = tokenName;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.consecutiveFailures = consecutiveFailures;
        this.lockoutStatus = lockoutStatus != null ? lockoutStatus : LockoutStatus.NONE;
        this.nextRetryAllowedAt = nextRetryAllowedAt;
        this.lockoutExpiresAt = lockoutExpiresAt;
        this.firstAttemptTime = firstAttemptTime;
        this.lastAttemptTime = lastAttemptTime;
        this.retryHistory = retryHistory != null ? retryHistory : new ArrayList<>();
        this.lockoutWindowResetAt = lockoutWindowResetAt;
    }
    
    // Builder constructor
    private TokenRetryState(Builder builder) {
        this.tokenName = builder.tokenName;
        this.attemptCount = builder.attemptCount;
        this.maxAttempts = builder.maxAttempts;
        this.consecutiveFailures = builder.consecutiveFailures;
        this.lockoutStatus = builder.lockoutStatus;
        this.nextRetryAllowedAt = builder.nextRetryAllowedAt;
        this.lockoutExpiresAt = builder.lockoutExpiresAt;
        this.firstAttemptTime = builder.firstAttemptTime;
        this.lastAttemptTime = builder.lastAttemptTime;
        this.retryHistory = builder.retryHistory;
        this.lockoutWindowResetAt = builder.lockoutWindowResetAt;
    }
    
    // Getters
    public String getTokenName() {
        return tokenName;
    }
    
    public int getAttemptCount() {
        return attemptCount;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }
    
    public LockoutStatus getLockoutStatus() {
        return lockoutStatus;
    }
    
    public LocalDateTime getNextRetryAllowedAt() {
        return nextRetryAllowedAt;
    }
    
    public LocalDateTime getLockoutExpiresAt() {
        return lockoutExpiresAt;
    }
    
    public LocalDateTime getFirstAttemptTime() {
        return firstAttemptTime;
    }
    
    public LocalDateTime getLastAttemptTime() {
        return lastAttemptTime;
    }
    
    public List<RetryAttempt> getRetryHistory() {
        return new ArrayList<>(retryHistory);
    }
    
    public LocalDateTime getLockoutWindowResetAt() {
        return lockoutWindowResetAt;
    }
    
    // Setters for mutable operations
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public void setLockoutStatus(LockoutStatus lockoutStatus) {
        this.lockoutStatus = lockoutStatus;
    }
    
    public void setNextRetryAllowedAt(LocalDateTime nextRetryAllowedAt) {
        this.nextRetryAllowedAt = nextRetryAllowedAt;
    }
    
    public void setLockoutExpiresAt(LocalDateTime lockoutExpiresAt) {
        this.lockoutExpiresAt = lockoutExpiresAt;
    }
    
    public void setLockoutWindowResetAt(LocalDateTime lockoutWindowResetAt) {
        this.lockoutWindowResetAt = lockoutWindowResetAt;
    }
    
    // Business logic methods
    public boolean canRetryNow() {
        LocalDateTime now = LocalDateTime.now();
        
        // Check if locked out
        if (lockoutStatus == LockoutStatus.LOCKED_OUT) {
            return lockoutExpiresAt != null && now.isAfter(lockoutExpiresAt);
        }
        
        // Check if permanently failed
        if (lockoutStatus == LockoutStatus.PERMANENTLY_FAILED) {
            return false;
        }
        
        // Check if in retry delay
        if (lockoutStatus == LockoutStatus.RETRY_DELAY) {
            return nextRetryAllowedAt == null || now.isAfter(nextRetryAllowedAt);
        }
        
        // Check if still have attempts remaining
        return attemptCount < maxAttempts;
    }
    
    public boolean hasAttemptsRemaining() {
        return attemptCount < maxAttempts;
    }
    
    public int getRemainingAttempts() {
        return Math.max(0, maxAttempts - attemptCount);
    }
    
    public void recordAttempt(boolean success, String details) {
        LocalDateTime now = LocalDateTime.now();
        
        if (firstAttemptTime == null) {
            firstAttemptTime = now;
        }
        lastAttemptTime = now;
        attemptCount++;
        
        RetryAttempt attempt = new RetryAttempt(attemptCount, now, success, details);
        retryHistory.add(attempt);
        
        if (!success) {
            consecutiveFailures++;
        } else {
            consecutiveFailures = 0; // Reset on success
        }
    }
    
    public void resetAttempts() {
        attemptCount = 0;
        consecutiveFailures = 0;
        lockoutStatus = LockoutStatus.NONE;
        nextRetryAllowedAt = null;
        lockoutExpiresAt = null;
        retryHistory.clear();
    }
    
    public boolean shouldResetWindow() {
        return lockoutWindowResetAt != null && LocalDateTime.now().isAfter(lockoutWindowResetAt);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String tokenName;
        private int attemptCount = 0;
        private int maxAttempts = 3;
        private int consecutiveFailures = 0;
        private LockoutStatus lockoutStatus = LockoutStatus.NONE;
        private LocalDateTime nextRetryAllowedAt;
        private LocalDateTime lockoutExpiresAt;
        private LocalDateTime firstAttemptTime;
        private LocalDateTime lastAttemptTime;
        private List<RetryAttempt> retryHistory = new ArrayList<>();
        private LocalDateTime lockoutWindowResetAt;
        
        public Builder tokenName(String tokenName) {
            this.tokenName = tokenName;
            return this;
        }
        
        public Builder attemptCount(int attemptCount) {
            this.attemptCount = attemptCount;
            return this;
        }
        
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public Builder consecutiveFailures(int consecutiveFailures) {
            this.consecutiveFailures = consecutiveFailures;
            return this;
        }
        
        public Builder lockoutStatus(LockoutStatus lockoutStatus) {
            this.lockoutStatus = lockoutStatus;
            return this;
        }
        
        public Builder nextRetryAllowedAt(LocalDateTime nextRetryAllowedAt) {
            this.nextRetryAllowedAt = nextRetryAllowedAt;
            return this;
        }
        
        public Builder lockoutExpiresAt(LocalDateTime lockoutExpiresAt) {
            this.lockoutExpiresAt = lockoutExpiresAt;
            return this;
        }
        
        public Builder firstAttemptTime(LocalDateTime firstAttemptTime) {
            this.firstAttemptTime = firstAttemptTime;
            return this;
        }
        
        public Builder lastAttemptTime(LocalDateTime lastAttemptTime) {
            this.lastAttemptTime = lastAttemptTime;
            return this;
        }
        
        public Builder retryHistory(List<RetryAttempt> retryHistory) {
            this.retryHistory = retryHistory != null ? new ArrayList<>(retryHistory) : new ArrayList<>();
            return this;
        }
        
        public Builder lockoutWindowResetAt(LocalDateTime lockoutWindowResetAt) {
            this.lockoutWindowResetAt = lockoutWindowResetAt;
            return this;
        }
        
        public TokenRetryState build() {
            Objects.requireNonNull(tokenName, "Token name is required");
            return new TokenRetryState(this);
        }
    }
    
    /**
     * Inner class to represent individual retry attempts.
     */
    public static class RetryAttempt {
        @JsonProperty("attemptNumber")
        private final int attemptNumber;
        
        @JsonProperty("timestamp")
        private final LocalDateTime timestamp;
        
        @JsonProperty("success")
        private final boolean success;
        
        @JsonProperty("details")
        private final String details;
        
        @JsonCreator
        public RetryAttempt(
                @JsonProperty("attemptNumber") int attemptNumber,
                @JsonProperty("timestamp") LocalDateTime timestamp,
                @JsonProperty("success") boolean success,
                @JsonProperty("details") String details) {
            this.attemptNumber = attemptNumber;
            this.timestamp = timestamp;
            this.success = success;
            this.details = details;
        }
        
        public int getAttemptNumber() {
            return attemptNumber;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getDetails() {
            return details;
        }
        
        @Override
        public String toString() {
            return "RetryAttempt{" +
                    "attemptNumber=" + attemptNumber +
                    ", timestamp=" + timestamp +
                    ", success=" + success +
                    ", details='" + details + '\'' +
                    '}';
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenRetryState that = (TokenRetryState) o;
        return Objects.equals(tokenName, that.tokenName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tokenName);
    }
    
    @Override
    public String toString() {
        return "TokenRetryState{" +
                "tokenName='" + tokenName + '\'' +
                ", attemptCount=" + attemptCount +
                ", maxAttempts=" + maxAttempts +
                ", consecutiveFailures=" + consecutiveFailures +
                ", lockoutStatus=" + lockoutStatus +
                ", remainingAttempts=" + getRemainingAttempts() +
                '}';
    }
} 