package com.bank.ivr.auth.repository.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.repository.AuthenticationContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of AuthenticationContextRepository.
 * Used when Redis is not available or for testing purposes.
 */
@Repository
@Primary
@ConditionalOnMissingBean(RedisTemplate.class)
public class InMemoryAuthenticationContextRepository implements AuthenticationContextRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryAuthenticationContextRepository.class);
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    
    private final Map<String, AuthenticationContextEntry> storage = new ConcurrentHashMap<>();
    
    /**
     * Internal class to store context with expiration time
     */
    private static class AuthenticationContextEntry {
        private final AuthenticationContext context;
        private final LocalDateTime expiresAt;
        
        public AuthenticationContextEntry(AuthenticationContext context, Duration ttl) {
            this.context = context;
            this.expiresAt = LocalDateTime.now().plus(ttl);
        }
        
        public AuthenticationContext getContext() {
            return context;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
    
    @Override
    public void save(AuthenticationContext context) {
        save(context, DEFAULT_TTL);
    }
    
    @Override
    public void save(AuthenticationContext context, Duration ttl) {
        String key = context.getAttemptId();
        storage.put(key, new AuthenticationContextEntry(context, ttl));
        logger.debug("Saved authentication context for attempt: {}", context.getAttemptId());
    }
    
    @Override
    public Optional<AuthenticationContext> findByAttemptId(String attemptId) {
        AuthenticationContextEntry entry = storage.get(attemptId);
        
        if (entry == null) {
            logger.debug("No authentication context found for attempt: {}", attemptId);
            return Optional.empty();
        }
        
        if (entry.isExpired()) {
            storage.remove(attemptId);
            logger.debug("Authentication context expired for attempt: {}", attemptId);
            return Optional.empty();
        }
        
        logger.debug("Retrieved authentication context for attempt: {}", attemptId);
        return Optional.of(entry.getContext());
    }
    
    @Override
    public void deleteByAttemptId(String attemptId) {
        AuthenticationContextEntry removed = storage.remove(attemptId);
        
        if (removed != null) {
            logger.debug("Deleted authentication context for attempt: {}", attemptId);
        } else {
            logger.debug("No authentication context found to delete for attempt: {}", attemptId);
        }
    }
    
    @Override
    public boolean existsByAttemptId(String attemptId) {
        AuthenticationContextEntry entry = storage.get(attemptId);
        
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            storage.remove(attemptId);
            return false;
        }
        
        return true;
    }
    
    @Override
    public void update(AuthenticationContext context) {
        update(context, DEFAULT_TTL);
    }
    
    @Override
    public void update(AuthenticationContext context, Duration ttl) {
        // For in-memory storage, update is the same as save
        save(context, ttl);
    }
    
    @Override
    public void deleteExpiredContexts() {
        final int[] cleanedCount = {0};
        
        // Remove expired entries
        storage.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                cleanedCount[0]++;
                return true;
            }
            return false;
        });
        
        if (cleanedCount[0] > 0) {
            logger.debug("Cleaned up {} expired authentication contexts", cleanedCount[0]);
        }
    }
    
    /**
     * Get the current size of the storage (for testing/monitoring purposes)
     */
    public int size() {
        return storage.size();
    }
    
    /**
     * Clear all stored contexts (for testing purposes)
     */
    public void clear() {
        storage.clear();
        logger.debug("Cleared all authentication contexts from memory");
    }
} 