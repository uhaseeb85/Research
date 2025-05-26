package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.BrandGlobalRetryPolicy;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.GlobalRetryState;
import com.bank.ivr.auth.model.domain.TokenRetryState;
import com.bank.ivr.auth.model.domain.TokenRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive service for managing token and brand level retry strategies.
 * Handles retry delays, lockouts, escalation policies, and analytics.
 */
@Service
public class TokenRetryManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenRetryManagementService.class);
    
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public TokenRetryManagementService(BrandAuthConfigurationService brandConfigService) {
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Initializes retry state for a new authentication context.
     * 
     * @param context the authentication context
     * @param customerProfile the customer profile
     */
    public void initializeRetryState(AuthenticationContext context, CustomerProfile customerProfile) {
        String brand = context.getBrand();
        logger.debug("Initializing retry state for attempt: {}, brand: {}", context.getAttemptId(), brand);
        
        // Initialize token retry states
        Map<String, TokenRetryState> tokenRetryStates = new HashMap<>();
        Map<String, TokenRetryStrategy> retryStrategies = brandConfigService.getTokenRetryStrategies(brand);
        
        for (String tokenName : context.getEligibleTokens()) {
            TokenRetryStrategy strategy = retryStrategies.get(tokenName);
            if (strategy == null) {
                // Create default strategy if none specified
                strategy = createDefaultRetryStrategy(tokenName);
            }
            
            TokenRetryState retryState = TokenRetryState.builder()
                    .tokenName(tokenName)
                    .maxAttempts(strategy.getMaxRetries())
                    .lockoutWindowResetAt(LocalDateTime.now().plus(strategy.getResetWindowDuration()))
                    .build();
            
            tokenRetryStates.put(tokenName, retryState);
        }
        
        context.setTokenRetryStates(tokenRetryStates);
        
        // Initialize global retry state
        BrandGlobalRetryPolicy globalPolicy = brandConfigService.getGlobalRetryPolicy(brand);
        GlobalRetryState globalRetryState = GlobalRetryState.builder()
                .brand(brand)
                .windowResetTime(LocalDateTime.now().plus(globalPolicy.getRetryWindowResetDuration()))
                .build();
        
        context.setGlobalRetryState(globalRetryState);
        
        logger.debug("Initialized retry state with {} token strategies for brand: {}", 
                    tokenRetryStates.size(), brand);
    }
    
    /**
     * Validates if a token can be attempted based on retry policies.
     * 
     * @param context the authentication context
     * @param tokenName the token to validate
     * @return retry validation result
     */
    public RetryValidationResult validateTokenRetry(AuthenticationContext context, String tokenName) {
        String brand = context.getBrand();
        Map<String, TokenRetryState> tokenRetryStates = context.getTokenRetryStates();
        GlobalRetryState globalRetryState = context.getGlobalRetryState();
        
        // Check global lockout first
        if (!globalRetryState.canAuthenticate()) {
            logger.debug("Global lockout active for brand: {}, status: {}", 
                        brand, globalRetryState.getLockoutStatus());
            return RetryValidationResult.globalLockout(
                    "Authentication is temporarily locked for this brand",
                    globalRetryState.getLockoutExpiresAt()
            );
        }
        
        // Check token-specific state
        TokenRetryState tokenState = tokenRetryStates.get(tokenName);
        if (tokenState == null) {
            logger.warn("No retry state found for token: {} in brand: {}", tokenName, brand);
            return RetryValidationResult.denied("Token retry state not found");
        }
        
        // Check if token retry window should be reset
        if (tokenState.shouldResetWindow()) {
            resetTokenRetryWindow(tokenState, brand, tokenName);
        }
        
        // Check token-specific retry eligibility
        if (!tokenState.canRetryNow()) {
            logger.debug("Token retry denied for: {} in brand: {}, status: {}", 
                        tokenName, brand, tokenState.getLockoutStatus());
            
            return RetryValidationResult.tokenLocked(
                    String.format("Token %s is temporarily locked", tokenName),
                    tokenState.getNextRetryAllowedAt(),
                    tokenState.getLockoutExpiresAt()
            );
        }
        
        // Calculate any required delay
        Map<String, TokenRetryStrategy> retryStrategies = brandConfigService.getTokenRetryStrategies(brand);
        TokenRetryStrategy strategy = retryStrategies.get(tokenName);
        
        if (strategy != null) {
            long delayMs = calculateRetryDelay(tokenState, strategy, globalRetryState);
            if (delayMs > 0) {
                LocalDateTime nextRetryAt = LocalDateTime.now().plusNanos(delayMs * 1_000_000);
                logger.debug("Retry delay calculated for token: {} in brand: {}, delay: {}ms", 
                            tokenName, brand, delayMs);
                
                return RetryValidationResult.delayed(
                        String.format("Please wait %d seconds before retrying", delayMs / 1000),
                        nextRetryAt,
                        delayMs
                );
            }
        }
        
        return RetryValidationResult.allowed("Retry allowed");
    }
    
    /**
     * Records a token authentication attempt and updates retry state.
     * 
     * @param context the authentication context
     * @param tokenName the token that was attempted
     * @param success whether the attempt was successful
     * @param details additional details about the attempt
     */
    public void recordTokenAttempt(AuthenticationContext context, String tokenName, boolean success, String details) {
        String brand = context.getBrand();
        Map<String, TokenRetryState> tokenRetryStates = context.getTokenRetryStates();
        GlobalRetryState globalRetryState = context.getGlobalRetryState();
        
        TokenRetryState tokenState = tokenRetryStates.get(tokenName);
        if (tokenState == null) {
            logger.warn("No retry state found for token: {} when recording attempt", tokenName);
            return;
        }
        
        // Record the attempt
        tokenState.recordAttempt(success, details);
        
        if (success) {
            logger.debug("Successful authentication recorded for token: {} in brand: {}", tokenName, brand);
            globalRetryState.recordSuccess();
        } else {
            logger.debug("Failed authentication recorded for token: {} in brand: {}, attempts: {}/{}", 
                        tokenName, brand, tokenState.getAttemptCount(), tokenState.getMaxAttempts());
            
            globalRetryState.recordFailure();
            
            // Apply retry strategy
            applyRetryStrategy(context, tokenName, tokenState);
            
            // Check for escalation
            checkAndApplyEscalation(context, globalRetryState);
        }
        
        logger.debug("Updated retry state for token: {} - attempts: {}, global failures: {}", 
                    tokenName, tokenState.getAttemptCount(), globalRetryState.getTotalFailures());
    }
    
    /**
     * Applies retry strategy after a failed attempt.
     */
    private void applyRetryStrategy(AuthenticationContext context, String tokenName, TokenRetryState tokenState) {
        String brand = context.getBrand();
        Map<String, TokenRetryStrategy> retryStrategies = brandConfigService.getTokenRetryStrategies(brand);
        TokenRetryStrategy strategy = retryStrategies.get(tokenName);
        
        if (strategy == null) {
            strategy = createDefaultRetryStrategy(tokenName);
        }
        
        // Check if token has exhausted attempts
        if (!tokenState.hasAttemptsRemaining()) {
            if (strategy.isProgressiveLockoutEnabled()) {
                LocalDateTime lockoutExpiry = LocalDateTime.now().plus(strategy.getLockoutDurationAfterExhaustion());
                tokenState.setLockoutStatus(TokenRetryState.LockoutStatus.LOCKED_OUT);
                tokenState.setLockoutExpiresAt(lockoutExpiry);
                
                logger.info("Token {} locked out until {} after exhausting attempts in brand: {}", 
                           tokenName, lockoutExpiry, brand);
            } else {
                tokenState.setLockoutStatus(TokenRetryState.LockoutStatus.PERMANENTLY_FAILED);
                
                logger.info("Token {} permanently failed for session in brand: {}", tokenName, brand);
            }
        } else {
            // Apply retry delay
            GlobalRetryState globalState = context.getGlobalRetryState();
            long delayMs = calculateRetryDelay(tokenState, strategy, globalState);
            
            if (delayMs > 0) {
                LocalDateTime nextRetryAt = LocalDateTime.now().plusNanos(delayMs * 1_000_000);
                tokenState.setLockoutStatus(TokenRetryState.LockoutStatus.RETRY_DELAY);
                tokenState.setNextRetryAllowedAt(nextRetryAt);
                
                logger.debug("Retry delay applied to token {} in brand: {}, next retry at: {}", 
                            tokenName, brand, nextRetryAt);
            }
        }
    }
    
    /**
     * Calculates retry delay based on strategy and global state.
     */
    private long calculateRetryDelay(TokenRetryState tokenState, TokenRetryStrategy strategy, GlobalRetryState globalState) {
        long baseDelay = strategy.calculateDelayMs(tokenState.getAttemptCount() - 1);
        
        // Apply cross-token delay multiplier from global state
        double globalMultiplier = globalState.getCrossTokenDelayMultiplier();
        
        return Math.round(baseDelay * globalMultiplier);
    }
    
    /**
     * Checks for and applies escalation policies.
     */
    private void checkAndApplyEscalation(AuthenticationContext context, GlobalRetryState globalState) {
        String brand = context.getBrand();
        BrandGlobalRetryPolicy globalPolicy = brandConfigService.getGlobalRetryPolicy(brand);
        
        // Check for suspicious activity
        if (globalPolicy.shouldTriggerSuspiciousActivityLockout(globalState.getRapidFailureCount())) {
            LocalDateTime lockoutExpiry = LocalDateTime.now().plus(globalPolicy.getSuspiciousActivityLockoutDuration());
            globalState.triggerLockout(GlobalRetryState.GlobalLockoutStatus.SUSPICIOUS_ACTIVITY, lockoutExpiry);
            globalState.setSuspiciousActivityDetected(true);
            
            logger.warn("Suspicious activity detected for brand: {}, rapid failures: {}, locked until: {}", 
                       brand, globalState.getRapidFailureCount(), lockoutExpiry);
        }
        
        // Check for global lockout
        else if (globalPolicy.shouldTriggerGlobalLockout(globalState.getTotalFailures())) {
            LocalDateTime lockoutExpiry = LocalDateTime.now().plus(globalPolicy.getGlobalLockoutDuration());
            globalState.triggerLockout(GlobalRetryState.GlobalLockoutStatus.HARD_LOCKOUT, lockoutExpiry);
            
            logger.warn("Global lockout triggered for brand: {}, total failures: {}, locked until: {}", 
                       brand, globalState.getTotalFailures(), lockoutExpiry);
        }
        
        // Check for escalation
        else if (globalPolicy.shouldTriggerEscalation(globalState.getConsecutiveFailures())) {
            globalState.escalate();
            
            logger.info("Escalation triggered for brand: {}, level: {}, consecutive failures: {}", 
                       brand, globalState.getEscalationLevel(), globalState.getConsecutiveFailures());
        }
    }
    
    /**
     * Resets token retry window when the reset period has elapsed.
     */
    private void resetTokenRetryWindow(TokenRetryState tokenState, String brand, String tokenName) {
        logger.debug("Resetting retry window for token: {} in brand: {}", tokenName, brand);
        
        tokenState.resetAttempts();
        Map<String, TokenRetryStrategy> retryStrategies = brandConfigService.getTokenRetryStrategies(brand);
        TokenRetryStrategy strategy = retryStrategies.get(tokenName);
        
        if (strategy != null) {
            tokenState.setLockoutWindowResetAt(LocalDateTime.now().plus(strategy.getResetWindowDuration()));
        }
    }
    
    /**
     * Creates a default retry strategy for tokens without specific configuration.
     */
    private TokenRetryStrategy createDefaultRetryStrategy(String tokenName) {
        return TokenRetryStrategy.builder()
                .tokenName(tokenName)
                .retryType(TokenRetryStrategy.RetryType.FIXED_DELAY)
                .maxRetries(3)
                .baseDelayMs(1000)
                .progressiveLockoutEnabled(true)
                .build();
    }
    
    /**
     * Gets comprehensive retry analytics for monitoring.
     * 
     * @param context the authentication context
     * @return retry analytics data
     */
    public RetryAnalytics getRetryAnalytics(AuthenticationContext context) {
        return RetryAnalytics.builder()
                .brand(context.getBrand())
                .attemptId(context.getAttemptId())
                .tokenRetryStates(context.getTokenRetryStates())
                .globalRetryState(context.getGlobalRetryState())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Result class for retry validation.
     */
    public static class RetryValidationResult {
        public enum Status {
            ALLOWED, DENIED, DELAYED, TOKEN_LOCKED, GLOBAL_LOCKED
        }
        
        private final Status status;
        private final String message;
        private final LocalDateTime nextRetryAt;
        private final LocalDateTime lockoutExpiresAt;
        private final long delayMs;
        
        private RetryValidationResult(Status status, String message, LocalDateTime nextRetryAt, 
                                     LocalDateTime lockoutExpiresAt, long delayMs) {
            this.status = status;
            this.message = message;
            this.nextRetryAt = nextRetryAt;
            this.lockoutExpiresAt = lockoutExpiresAt;
            this.delayMs = delayMs;
        }
        
        public static RetryValidationResult allowed(String message) {
            return new RetryValidationResult(Status.ALLOWED, message, null, null, 0);
        }
        
        public static RetryValidationResult denied(String message) {
            return new RetryValidationResult(Status.DENIED, message, null, null, 0);
        }
        
        public static RetryValidationResult delayed(String message, LocalDateTime nextRetryAt, long delayMs) {
            return new RetryValidationResult(Status.DELAYED, message, nextRetryAt, null, delayMs);
        }
        
        public static RetryValidationResult tokenLocked(String message, LocalDateTime nextRetryAt, LocalDateTime lockoutExpiresAt) {
            return new RetryValidationResult(Status.TOKEN_LOCKED, message, nextRetryAt, lockoutExpiresAt, 0);
        }
        
        public static RetryValidationResult globalLockout(String message, LocalDateTime lockoutExpiresAt) {
            return new RetryValidationResult(Status.GLOBAL_LOCKED, message, null, lockoutExpiresAt, 0);
        }
        
        // Getters
        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getNextRetryAt() { return nextRetryAt; }
        public LocalDateTime getLockoutExpiresAt() { return lockoutExpiresAt; }
        public long getDelayMs() { return delayMs; }
        
        public boolean isAllowed() { return status == Status.ALLOWED; }
        public boolean isDelayed() { return status == Status.DELAYED; }
        public boolean isLocked() { return status == Status.TOKEN_LOCKED || status == Status.GLOBAL_LOCKED; }
    }
    
    /**
     * Analytics data class for retry monitoring.
     */
    public static class RetryAnalytics {
        private final String brand;
        private final String attemptId;
        private final Map<String, TokenRetryState> tokenRetryStates;
        private final GlobalRetryState globalRetryState;
        private final LocalDateTime timestamp;
        
        private RetryAnalytics(Builder builder) {
            this.brand = builder.brand;
            this.attemptId = builder.attemptId;
            this.tokenRetryStates = builder.tokenRetryStates;
            this.globalRetryState = builder.globalRetryState;
            this.timestamp = builder.timestamp;
        }
        
        public static Builder builder() { return new Builder(); }
        
        // Getters
        public String getBrand() { return brand; }
        public String getAttemptId() { return attemptId; }
        public Map<String, TokenRetryState> getTokenRetryStates() { return tokenRetryStates; }
        public GlobalRetryState getGlobalRetryState() { return globalRetryState; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public static class Builder {
            private String brand;
            private String attemptId;
            private Map<String, TokenRetryState> tokenRetryStates;
            private GlobalRetryState globalRetryState;
            private LocalDateTime timestamp;
            
            public Builder brand(String brand) { this.brand = brand; return this; }
            public Builder attemptId(String attemptId) { this.attemptId = attemptId; return this; }
            public Builder tokenRetryStates(Map<String, TokenRetryState> tokenRetryStates) { 
                this.tokenRetryStates = tokenRetryStates; return this; 
            }
            public Builder globalRetryState(GlobalRetryState globalRetryState) { 
                this.globalRetryState = globalRetryState; return this; 
            }
            public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
            
            public RetryAnalytics build() { return new RetryAnalytics(this); }
        }
    }
} 