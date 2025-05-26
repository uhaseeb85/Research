package com.bank.ivr.auth.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Utility class for standardized logging patterns across the IVR Authentication System.
 * Provides consistent log formatting, structured logging, and context management.
 */
public class LoggingUtil {
    
    // Standard MDC keys for consistent context
    public static final String SESSION_ID = "sessionId";
    public static final String ATTEMPT_ID = "attemptId";
    public static final String BRAND = "brand";
    public static final String CUSTOMER_ID = "customerId";
    public static final String TOKEN_NAME = "tokenName";
    public static final String OPERATION = "operation";
    
    /**
     * Sets authentication context in MDC for structured logging.
     */
    public static void setAuthContext(String sessionId, String attemptId, String brand) {
        if (sessionId != null) MDC.put(SESSION_ID, sessionId);
        if (attemptId != null) MDC.put(ATTEMPT_ID, attemptId);
        if (brand != null) MDC.put(BRAND, brand);
    }
    
    /**
     * Sets customer context in MDC.
     */
    public static void setCustomerContext(String customerId) {
        if (customerId != null) MDC.put(CUSTOMER_ID, customerId);
    }
    
    /**
     * Sets token context in MDC.
     */
    public static void setTokenContext(String tokenName) {
        if (tokenName != null) MDC.put(TOKEN_NAME, tokenName);
    }
    
    /**
     * Sets operation context in MDC.
     */
    public static void setOperationContext(String operation) {
        if (operation != null) MDC.put(OPERATION, operation);
    }
    
    /**
     * Clears all authentication-related MDC context.
     */
    public static void clearAuthContext() {
        MDC.remove(SESSION_ID);
        MDC.remove(ATTEMPT_ID);
        MDC.remove(BRAND);
        MDC.remove(CUSTOMER_ID);
        MDC.remove(TOKEN_NAME);
        MDC.remove(OPERATION);
    }
    
    /**
     * Logs authentication start with standard format.
     */
    public static void logAuthStart(Logger logger, String sessionId, String brand, String customerIdentifier) {
        setAuthContext(sessionId, null, brand);
        logger.info("Authentication request started - SessionId: {}, Brand: {}, Customer: {}", 
                   sessionId, brand, customerIdentifier);
    }
    
    /**
     * Logs authentication completion with standard format.
     */
    public static void logAuthComplete(Logger logger, String sessionId, String brand, String status, long processingTimeMs) {
        logger.info("Authentication completed - SessionId: {}, Brand: {}, Status: {}, ProcessingTime: {}ms", 
                   sessionId, brand, status, processingTimeMs);
        clearAuthContext();
    }
    
    /**
     * Logs token validation with standard format.
     */
    public static void logTokenValidation(Logger logger, String tokenName, String brand, String customerId, boolean success) {
        setTokenContext(tokenName);
        setCustomerContext(customerId);
        logger.debug("Token '{}' validation {} for brand '{}' and customer '{}'", 
                    tokenName, success ? "successful" : "failed", brand, customerId);
    }
    
    /**
     * Logs validator registration with standard format.
     */
    public static void logValidatorRegistration(Logger logger, String brand, String tokenName, int priority, String validatorClass) {
        logger.debug("Registered validator for brand '{}', token '{}' with priority {} - Class: {}", 
                    brand, tokenName, priority, validatorClass);
    }
    
    /**
     * Logs brand configuration loading with standard format.
     */
    public static void logBrandConfigLoaded(Logger logger, String brand, int tokenCount, int priority) {
        logger.debug("Loaded brand configuration - Brand: {}, Tokens: {}, Priority: {}", 
                    brand, tokenCount, priority);
    }
    
    /**
     * Logs service initialization with standard format.
     */
    public static void logServiceInit(Logger logger, String serviceName, int componentCount, String componentType) {
        logger.info("Initialized {} with {} {}", serviceName, componentCount, componentType);
    }
    
    /**
     * Logs retry state changes with standard format.
     */
    public static void logRetryState(Logger logger, String tokenName, String brand, String action, Object details) {
        setTokenContext(tokenName);
        logger.debug("Retry state - Token: {}, Brand: {}, Action: {}, Details: {}", 
                    tokenName, brand, action, details);
    }
    
    /**
     * Logs security events with standard format.
     */
    public static void logSecurityEvent(Logger logger, String event, String brand, String customerId, String details) {
        setCustomerContext(customerId);
        logger.warn("Security event - Event: {}, Brand: {}, Customer: {}, Details: {}", 
                   event, brand, customerId, details);
    }
    
    /**
     * Logs errors with standard format and context preservation.
     */
    public static void logError(Logger logger, String operation, String context, Exception e) {
        setOperationContext(operation);
        logger.error("Error in {} - Context: {}, Error: {}", operation, context, e.getMessage(), e);
    }
    
    /**
     * Logs performance metrics with standard format.
     */
    public static void logPerformance(Logger logger, String operation, long durationMs, String details) {
        setOperationContext(operation);
        logger.debug("Performance - Operation: {}, Duration: {}ms, Details: {}", 
                    operation, durationMs, details);
    }
    
    /**
     * Logs business logic decisions with standard format.
     */
    public static void logBusinessDecision(Logger logger, String decision, String reasoning, Object context) {
        logger.debug("Business decision - Decision: {}, Reasoning: {}, Context: {}", 
                    decision, reasoning, context);
    }
    
    /**
     * Logs configuration warnings with standard format.
     */
    public static void logConfigWarning(Logger logger, String component, String issue, String suggestion) {
        logger.warn("Configuration warning - Component: {}, Issue: {}, Suggestion: {}", 
                   component, issue, suggestion);
    }
    
    /**
     * Logs data access operations with standard format.
     */
    public static void logDataAccess(Logger logger, String operation, String entityType, String entityId, boolean success) {
        logger.debug("Data access - Operation: {}, Entity: {} ({}), Success: {}", 
                    operation, entityType, entityId, success);
    }
    
    /**
     * Creates a masked version of sensitive data for logging.
     */
    public static String maskSensitiveData(String data, int visibleChars) {
        if (data == null || data.length() <= visibleChars) {
            return "***";
        }
        return data.substring(0, visibleChars) + "***";
    }
    
    /**
     * Creates a masked version showing only last N characters.
     */
    public static String maskShowingLast(String data, int lastChars) {
        if (data == null || data.length() <= lastChars) {
            return "***";
        }
        return "***" + data.substring(data.length() - lastChars);
    }
    
    /**
     * Formats duration in a human-readable way.
     */
    public static String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.1fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
} 