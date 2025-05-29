package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks global retry state across all authentication tokens for a brand.
 * This includes overall failure counts, lockout status, and escalation tracking.
 */
public class GlobalRetryState {
    
    public enum GlobalLockoutStatus {
        NONE,                    // No global lockout active
        WARNING,                 // Close to triggering lockout
        SOFT_LOCKOUT,           // Temporary lockout with reduced attempts
        HARD_LOCKOUT,           // Full authentication lockout
        SUSPICIOUS_ACTIVITY,    // Flagged for suspicious patterns
        ESCALATED               // Escalated to higher security tier
    }
    
    @JsonProperty("brand")
    private final String brand;
    
    @JsonProperty("totalFailures")
    private int totalFailures;
    
    @JsonProperty("consecutiveFailures")
    private int consecutiveFailures;
    
    @JsonProperty("rapidFailureCount")
    private int rapidFailureCount;
    
    @JsonProperty("lockoutStatus")
    private GlobalLockoutStatus lockoutStatus;
    
    @JsonProperty("lockoutStartTime")
    private LocalDateTime lockoutStartTime;
    
    @JsonProperty("lockoutExpiresAt")
    private LocalDateTime lockoutExpiresAt;
    
    @JsonProperty("escalationLevel")
    private int escalationLevel;
    
    @JsonProperty("lastFailureTime")
    private LocalDateTime lastFailureTime;
    
    @JsonProperty("firstFailureTime")
    private LocalDateTime firstFailureTime;
    
    @JsonProperty("windowResetTime")
    private LocalDateTime windowResetTime;
    
    @JsonProperty("failureTimestamps")
    private List<LocalDateTime> failureTimestamps;
    
    @JsonProperty("suspiciousActivityDetected")
    private boolean suspiciousActivityDetected;
    
    @JsonProperty("crossTokenDelayMultiplier")
    private double crossTokenDelayMultiplier;
    
    @JsonCreator
    public GlobalRetryState(
            @JsonProperty("brand") String brand,
            @JsonProperty("totalFailures") int totalFailures,
            @JsonProperty("consecutiveFailures") int consecutiveFailures,
            @JsonProperty("rapidFailureCount") int rapidFailureCount,
            @JsonProperty("lockoutStatus") GlobalLockoutStatus lockoutStatus,
            @JsonProperty("lockoutStartTime") LocalDateTime lockoutStartTime,
            @JsonProperty("lockoutExpiresAt") LocalDateTime lockoutExpiresAt,
            @JsonProperty("escalationLevel") int escalationLevel,
            @JsonProperty("lastFailureTime") LocalDateTime lastFailureTime,
            @JsonProperty("firstFailureTime") LocalDateTime firstFailureTime,
            @JsonProperty("windowResetTime") LocalDateTime windowResetTime,
            @JsonProperty("failureTimestamps") List<LocalDateTime> failureTimestamps,
            @JsonProperty("suspiciousActivityDetected") boolean suspiciousActivityDetected,
            @JsonProperty("crossTokenDelayMultiplier") double crossTokenDelayMultiplier) {
        this.brand = brand;
        this.totalFailures = totalFailures;
        this.consecutiveFailures = consecutiveFailures;
        this.rapidFailureCount = rapidFailureCount;
        this.lockoutStatus = lockoutStatus != null ? lockoutStatus : GlobalLockoutStatus.NONE;
        this.lockoutStartTime = lockoutStartTime;
        this.lockoutExpiresAt = lockoutExpiresAt;
        this.escalationLevel = escalationLevel;
        this.lastFailureTime = lastFailureTime;
        this.firstFailureTime = firstFailureTime;
        this.windowResetTime = windowResetTime;
        this.failureTimestamps = failureTimestamps != null ? failureTimestamps : new ArrayList<>();
        this.suspiciousActivityDetected = suspiciousActivityDetected;
        this.crossTokenDelayMultiplier = crossTokenDelayMultiplier;
    }
    
    // Builder constructor
    private GlobalRetryState(Builder builder) {
        this.brand = builder.brand;
        this.totalFailures = builder.totalFailures;
        this.consecutiveFailures = builder.consecutiveFailures;
        this.rapidFailureCount = builder.rapidFailureCount;
        this.lockoutStatus = builder.lockoutStatus;
        this.lockoutStartTime = builder.lockoutStartTime;
        this.lockoutExpiresAt = builder.lockoutExpiresAt;
        this.escalationLevel = builder.escalationLevel;
        this.lastFailureTime = builder.lastFailureTime;
        this.firstFailureTime = builder.firstFailureTime;
        this.windowResetTime = builder.windowResetTime;
        this.failureTimestamps = builder.failureTimestamps;
        this.suspiciousActivityDetected = builder.suspiciousActivityDetected;
        this.crossTokenDelayMultiplier = builder.crossTokenDelayMultiplier;
    }
    
    // Getters
    public String getBrand() {
        return brand;
    }
    
    public int getTotalFailures() {
        return totalFailures;
    }
    
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }
    
    public int getRapidFailureCount() {
        return rapidFailureCount;
    }
    
    public GlobalLockoutStatus getLockoutStatus() {
        return lockoutStatus;
    }
    
    public LocalDateTime getLockoutStartTime() {
        return lockoutStartTime;
    }
    
    public LocalDateTime getLockoutExpiresAt() {
        return lockoutExpiresAt;
    }
    
    public int getEscalationLevel() {
        return escalationLevel;
    }
    
    public LocalDateTime getLastFailureTime() {
        return lastFailureTime;
    }
    
    public LocalDateTime getFirstFailureTime() {
        return firstFailureTime;
    }
    
    public LocalDateTime getWindowResetTime() {
        return windowResetTime;
    }
    
    public List<LocalDateTime> getFailureTimestamps() {
        return new ArrayList<>(failureTimestamps);
    }
    
    public boolean isSuspiciousActivityDetected() {
        return suspiciousActivityDetected;
    }
    
    public double getCrossTokenDelayMultiplier() {
        return crossTokenDelayMultiplier;
    }
    
    // Setters for mutable operations
    public void setLockoutStatus(GlobalLockoutStatus lockoutStatus) {
        this.lockoutStatus = lockoutStatus;
    }
    
    public void setLockoutStartTime(LocalDateTime lockoutStartTime) {
        this.lockoutStartTime = lockoutStartTime;
    }
    
    public void setLockoutExpiresAt(LocalDateTime lockoutExpiresAt) {
        this.lockoutExpiresAt = lockoutExpiresAt;
    }
    
    public void setEscalationLevel(int escalationLevel) {
        this.escalationLevel = escalationLevel;
    }
    
    public void setWindowResetTime(LocalDateTime windowResetTime) {
        this.windowResetTime = windowResetTime;
    }
    
    public void setSuspiciousActivityDetected(boolean suspiciousActivityDetected) {
        this.suspiciousActivityDetected = suspiciousActivityDetected;
    }
    
    public void setCrossTokenDelayMultiplier(double crossTokenDelayMultiplier) {
        this.crossTokenDelayMultiplier = crossTokenDelayMultiplier;
    }
    
    // Business logic methods
    public boolean isLocked() {
        return lockoutStatus == GlobalLockoutStatus.HARD_LOCKOUT || 
               lockoutStatus == GlobalLockoutStatus.SUSPICIOUS_ACTIVITY;
    }
    
    public boolean canAuthenticate() {
        LocalDateTime now = LocalDateTime.now();
        
        if (isLocked() && lockoutExpiresAt != null) {
            return now.isAfter(lockoutExpiresAt);
        }
        
        return lockoutStatus == GlobalLockoutStatus.NONE || 
               lockoutStatus == GlobalLockoutStatus.WARNING ||
               lockoutStatus == GlobalLockoutStatus.SOFT_LOCKOUT;
    }
    
    public void recordFailure() {
        LocalDateTime now = LocalDateTime.now();
        
        if (firstFailureTime == null) {
            firstFailureTime = now;
        }
        
        lastFailureTime = now;
        totalFailures++;
        consecutiveFailures++;
        
        // Track failure timestamps for rapid failure detection
        failureTimestamps.add(now);
        
        // Remove old timestamps (older than 5 minutes for rapid failure detection)
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
        Iterator<LocalDateTime> iterator = failureTimestamps.iterator();
        while (iterator.hasNext()) {
            LocalDateTime timestamp = iterator.next();
            if (timestamp.isBefore(fiveMinutesAgo)) {
                iterator.remove();
            }
        }
        
        rapidFailureCount = failureTimestamps.size();
    }
    
    public void recordSuccess() {
        consecutiveFailures = 0; // Reset consecutive failures on success
        rapidFailureCount = 0; // Reset rapid failures on success
        failureTimestamps.clear();
        
        // Don't reset total failures as they're used for long-term tracking
    }
    
    public void triggerLockout(GlobalLockoutStatus status, LocalDateTime expiresAt) {
        this.lockoutStatus = status;
        this.lockoutStartTime = LocalDateTime.now();
        this.lockoutExpiresAt = expiresAt;
    }
    
    public void escalate() {
        escalationLevel++;
        crossTokenDelayMultiplier = Math.min(crossTokenDelayMultiplier * 1.5, 10.0); // Cap at 10x
    }
    
    public void resetWindow() {
        totalFailures = 0;
        consecutiveFailures = 0;
        rapidFailureCount = 0;
        lockoutStatus = GlobalLockoutStatus.NONE;
        lockoutStartTime = null;
        lockoutExpiresAt = null;
        escalationLevel = 0;
        failureTimestamps.clear();
        suspiciousActivityDetected = false;
        crossTokenDelayMultiplier = 1.0;
        windowResetTime = LocalDateTime.now();
    }
    
    public boolean shouldResetWindow(LocalDateTime resetThreshold) {
        return windowResetTime != null && LocalDateTime.now().isAfter(resetThreshold);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String brand;
        private int totalFailures = 0;
        private int consecutiveFailures = 0;
        private int rapidFailureCount = 0;
        private GlobalLockoutStatus lockoutStatus = GlobalLockoutStatus.NONE;
        private LocalDateTime lockoutStartTime;
        private LocalDateTime lockoutExpiresAt;
        private int escalationLevel = 0;
        private LocalDateTime lastFailureTime;
        private LocalDateTime firstFailureTime;
        private LocalDateTime windowResetTime;
        private List<LocalDateTime> failureTimestamps = new ArrayList<>();
        private boolean suspiciousActivityDetected = false;
        private double crossTokenDelayMultiplier = 1.0;
        
        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }
        
        public Builder totalFailures(int totalFailures) {
            this.totalFailures = totalFailures;
            return this;
        }
        
        public Builder consecutiveFailures(int consecutiveFailures) {
            this.consecutiveFailures = consecutiveFailures;
            return this;
        }
        
        public Builder rapidFailureCount(int rapidFailureCount) {
            this.rapidFailureCount = rapidFailureCount;
            return this;
        }
        
        public Builder lockoutStatus(GlobalLockoutStatus lockoutStatus) {
            this.lockoutStatus = lockoutStatus;
            return this;
        }
        
        public Builder lockoutStartTime(LocalDateTime lockoutStartTime) {
            this.lockoutStartTime = lockoutStartTime;
            return this;
        }
        
        public Builder lockoutExpiresAt(LocalDateTime lockoutExpiresAt) {
            this.lockoutExpiresAt = lockoutExpiresAt;
            return this;
        }
        
        public Builder escalationLevel(int escalationLevel) {
            this.escalationLevel = escalationLevel;
            return this;
        }
        
        public Builder lastFailureTime(LocalDateTime lastFailureTime) {
            this.lastFailureTime = lastFailureTime;
            return this;
        }
        
        public Builder firstFailureTime(LocalDateTime firstFailureTime) {
            this.firstFailureTime = firstFailureTime;
            return this;
        }
        
        public Builder windowResetTime(LocalDateTime windowResetTime) {
            this.windowResetTime = windowResetTime;
            return this;
        }
        
        public Builder failureTimestamps(List<LocalDateTime> failureTimestamps) {
            this.failureTimestamps = failureTimestamps != null ? new ArrayList<>(failureTimestamps) : new ArrayList<>();
            return this;
        }
        
        public Builder suspiciousActivityDetected(boolean suspiciousActivityDetected) {
            this.suspiciousActivityDetected = suspiciousActivityDetected;
            return this;
        }
        
        public Builder crossTokenDelayMultiplier(double crossTokenDelayMultiplier) {
            this.crossTokenDelayMultiplier = crossTokenDelayMultiplier;
            return this;
        }
        
        public GlobalRetryState build() {
            return new GlobalRetryState(this);
        }
    }
    
    @Override
    public String toString() {
        return "GlobalRetryState{" +
                "brand='" + brand + '\'' +
                ", totalFailures=" + totalFailures +
                ", consecutiveFailures=" + consecutiveFailures +
                ", lockoutStatus=" + lockoutStatus +
                ", escalationLevel=" + escalationLevel +
                ", suspiciousActivityDetected=" + suspiciousActivityDetected +
                '}';
    }
} 