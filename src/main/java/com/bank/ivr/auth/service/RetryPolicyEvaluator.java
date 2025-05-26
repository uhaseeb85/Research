package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;
import com.bank.ivr.auth.model.domain.GlobalRetryState;
import com.bank.ivr.auth.model.domain.TokenRetryState;
import com.bank.ivr.auth.model.domain.TokenRetryStrategy;
import com.bank.ivr.auth.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for evaluating retry policies and calculating delays.
 * Extracted from TokenRetryManagementService to improve separation of concerns.
 */
@Service
public class RetryPolicyEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryPolicyEvaluator.class);
    
    /**
     * Evaluates if a token can be retried based on its current state and strategy.
     */
    public boolean canRetryToken(TokenRetryState tokenState, TokenRetryStrategy strategy) {
        if (tokenState == null || strategy == null) {
            return false;
        }
        
        // Check if token retry window should be reset
        if (tokenState.shouldResetWindow()) {
            LoggingUtil.logRetryState(logger, tokenState.getTokenName(), "UNKNOWN", 
                    "WINDOW_RESET", "Retry window reset");
            return true;
        }
        
        return tokenState.canRetryNow();
    }
    
    /**
     * Evaluates if global retry policies allow authentication.
     */
    public boolean canRetryGlobally(GlobalRetryState globalState, BrandGlobalRetryPolicy policy) {
        if (globalState == null || policy == null) {
            return true; // Default to allowing if no policy
        }
        
        return globalState.canAuthenticate();
    }
    
    /**
     * Calculates the retry delay for a token based on its strategy and current state.
     */
    public long calculateRetryDelay(TokenRetryState tokenState, TokenRetryStrategy strategy, GlobalRetryState globalState) {
        if (tokenState == null || strategy == null) {
            return 0;
        }
        
        long baseDelay = strategy.getBaseDelayMs();
        int attemptCount = tokenState.getAttemptCount();
        
        long calculatedDelay = switch (strategy.getRetryType()) {
            case IMMEDIATE -> 0;
            case FIXED_DELAY -> baseDelay;
            case LINEAR_BACKOFF -> baseDelay * attemptCount;
            case EXPONENTIAL_BACKOFF -> (long) (baseDelay * Math.pow(strategy.getMultiplier(), attemptCount - 1));
        };
        
        // Apply global failure multiplier for cross-token delays
        if (globalState != null && globalState.getTotalFailures() > 1) {
            double multiplier = 1.0 + (globalState.getTotalFailures() * 0.1); // 10% increase per failure
            calculatedDelay = (long) (calculatedDelay * multiplier);
        }
        
        LoggingUtil.logRetryState(logger, tokenState.getTokenName(), "UNKNOWN", 
                "DELAY_CALCULATED", String.format("Delay: %dms, Type: %s, Attempts: %d", 
                calculatedDelay, strategy.getRetryType(), attemptCount));
        
        return Math.max(0, calculatedDelay);
    }
    
    /**
     * Evaluates if escalation should be triggered based on global state and policy.
     */
    public boolean shouldTriggerEscalation(GlobalRetryState globalState, BrandGlobalRetryPolicy policy) {
        if (globalState == null || policy == null) {
            return false;
        }
        
        return globalState.getTotalFailures() >= policy.getEscalationThreshold() &&
               policy.getEscalationPolicy() != BrandGlobalRetryPolicy.EscalationPolicy.NONE;
    }
    
    /**
     * Evaluates if suspicious activity lockout should be triggered.
     */
    public boolean shouldTriggerSuspiciousActivityLockout(GlobalRetryState globalState, BrandGlobalRetryPolicy policy) {
        if (globalState == null || policy == null) {
            return false;
        }
        
        return globalState.getTotalFailures() >= policy.getSuspiciousActivityThreshold();
    }
    
    /**
     * Evaluates if global lockout should be triggered.
     */
    public boolean shouldTriggerGlobalLockout(GlobalRetryState globalState, BrandGlobalRetryPolicy policy) {
        if (globalState == null || policy == null) {
            return false;
        }
        
        return globalState.getTotalFailures() >= policy.getGlobalLockoutThreshold();
    }
    
    /**
     * Determines the appropriate lockout duration based on policy and current state.
     */
    public LocalDateTime calculateLockoutExpiry(TokenRetryStrategy strategy, int attemptCount) {
        if (strategy == null || !strategy.isProgressiveLockoutEnabled()) {
            return strategy != null ? 
                    LocalDateTime.now().plus(strategy.getLockoutDurationAfterExhaustion()) :
                    LocalDateTime.now().plusMinutes(5); // Default 5 minutes
        }
        
        // Progressive lockout: increase duration with each attempt
        long baseMinutes = strategy.getLockoutDurationAfterExhaustion().toMinutes();
        long progressiveMinutes = baseMinutes * (long) Math.pow(2, Math.min(attemptCount - 1, 5)); // Cap at 2^5
        
        return LocalDateTime.now().plusMinutes(progressiveMinutes);
    }
    

    
    /**
     * Evaluates the severity of the current retry situation.
     */
    public RetrySeverity evaluateRetrySeverity(TokenRetryState tokenState, GlobalRetryState globalState, 
                                               TokenRetryStrategy strategy, BrandGlobalRetryPolicy globalPolicy) {
        
        if (shouldTriggerGlobalLockout(globalState, globalPolicy)) {
            return RetrySeverity.CRITICAL;
        }
        
        if (shouldTriggerSuspiciousActivityLockout(globalState, globalPolicy)) {
            return RetrySeverity.HIGH;
        }
        
        if (shouldTriggerEscalation(globalState, globalPolicy)) {
            return RetrySeverity.MEDIUM;
        }
        
        if (tokenState != null && tokenState.getAttemptCount() >= strategy.getMaxRetries() * 0.8) {
            return RetrySeverity.LOW;
        }
        
        return RetrySeverity.NORMAL;
    }
    
    /**
     * Enumeration of retry severity levels.
     */
    public enum RetrySeverity {
        NORMAL,    // Normal operation
        LOW,       // Approaching limits
        MEDIUM,    // Escalation triggered
        HIGH,      // Suspicious activity
        CRITICAL   // Global lockout imminent
    }
} 