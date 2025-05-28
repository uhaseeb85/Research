package com.bank.ivr.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing session context data.
 * In a real implementation, this would retrieve data from Redis or another session store.
 * For testing purposes, it loads data from a JSON file.
 */
@Service
public class SessionContextService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionContextService.class);
    
    private final ObjectMapper objectMapper;
    private Map<String, SessionContext> sessionContexts = new HashMap<>();
    
    public SessionContextService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void loadSampleContextData() {
        try {
            ClassPathResource resource = new ClassPathResource("sample-session-contexts.json");
            List<SessionContext> contexts = objectMapper.readValue(
                resource.getInputStream(), 
                new TypeReference<List<SessionContext>>() {}
            );
            
            for (SessionContext context : contexts) {
                sessionContexts.put(context.getSessionId(), context);
            }
            
            logger.info("Loaded {} sample session contexts", contexts.size());
        } catch (IOException e) {
            logger.warn("Could not load sample session contexts, using empty data: {}", e.getMessage());
        }
    }
    
    /**
     * Retrieves session context for a given session ID.
     * In a real implementation, this would query Redis or another session store.
     * 
     * @param sessionId the session ID
     * @return Optional containing the session context if found
     */
    public Optional<SessionContext> getSessionContext(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warn("Session ID is null or empty");
            return Optional.empty();
        }
        
        SessionContext context = sessionContexts.get(sessionId);
        if (context != null) {
            logger.debug("Found session context for session: {}", sessionId);
            return Optional.of(context);
        } else {
            logger.debug("No session context found for session: {}", sessionId);
            return Optional.empty();
        }
    }
    
    /**
     * Retrieves DNIS from session context.
     * 
     * @param sessionId the session ID
     * @return Optional containing the DNIS if found
     */
    public Optional<String> getDnisFromSession(String sessionId) {
        return getSessionContext(sessionId)
                .map(SessionContext::getDnis);
    }
    
    /**
     * Retrieves session SSN list from session context.
     * 
     * @param sessionId the session ID
     * @return Optional containing the list of SSNs if found
     */
    public Optional<List<String>> getSessionSsnFromSession(String sessionId) {
        return getSessionContext(sessionId)
                .map(SessionContext::getSessionSsn);
    }
    
    /**
     * Stores session context (for testing purposes).
     * In a real implementation, this would store in Redis or another session store.
     * 
     * @param sessionContext the session context to store
     */
    public void storeSessionContext(SessionContext sessionContext) {
        sessionContexts.put(sessionContext.getSessionId(), sessionContext);
        logger.debug("Stored session context for session: {}", sessionContext.getSessionId());
    }
    
    /**
     * Represents session context data that would be populated by a previous API call.
     */
    public static class SessionContext {
        private String sessionId;
        private String dnis;
        private List<String> sessionSsn;
        private String callerPhoneNumber;
        private String previousApiCallId;
        private long timestamp;
        
        // Default constructor for Jackson
        public SessionContext() {}
        
        public SessionContext(String sessionId, String dnis, List<String> sessionSsn, 
                            String callerPhoneNumber, String previousApiCallId, long timestamp) {
            this.sessionId = sessionId;
            this.dnis = dnis;
            this.sessionSsn = sessionSsn;
            this.callerPhoneNumber = callerPhoneNumber;
            this.previousApiCallId = previousApiCallId;
            this.timestamp = timestamp;
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getDnis() { return dnis; }
        public void setDnis(String dnis) { this.dnis = dnis; }
        
        public List<String> getSessionSsn() { return sessionSsn; }
        public void setSessionSsn(List<String> sessionSsn) { this.sessionSsn = sessionSsn; }
        
        public String getCallerPhoneNumber() { return callerPhoneNumber; }
        public void setCallerPhoneNumber(String callerPhoneNumber) { this.callerPhoneNumber = callerPhoneNumber; }
        
        public String getPreviousApiCallId() { return previousApiCallId; }
        public void setPreviousApiCallId(String previousApiCallId) { this.previousApiCallId = previousApiCallId; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return "SessionContext{" +
                   "sessionId='" + sessionId + '\'' +
                   ", dnis='" + dnis + '\'' +
                   ", sessionSsn=" + (sessionSsn != null ? "[MASKED]" : null) +
                   ", callerPhoneNumber='" + callerPhoneNumber + '\'' +
                   ", previousApiCallId='" + previousApiCallId + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
} 