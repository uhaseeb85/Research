package com.bank.ivr.auth.repository.impl;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.repository.AuthenticationContextRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of AuthenticationContextRepository.
 * Uses Redis for high-performance, distributed storage of authentication contexts.
 */
@Repository
public class RedisAuthenticationContextRepository implements AuthenticationContextRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisAuthenticationContextRepository.class);
    private static final String KEY_PREFIX = "auth:context:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15); // 15 minutes default
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisAuthenticationContextRepository(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void save(AuthenticationContext context) {
        save(context, DEFAULT_TTL);
    }
    
    @Override
    public void save(AuthenticationContext context, Duration ttl) {
        try {
            String key = buildKey(context.getAttemptId());
            String jsonValue = objectMapper.writeValueAsString(context);
            
            redisTemplate.opsForValue().set(key, jsonValue, ttl.toMillis(), TimeUnit.MILLISECONDS);
            
            logger.debug("Saved authentication context for attempt: {}", context.getAttemptId());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing authentication context for attempt: {}", context.getAttemptId(), e);
            throw new RuntimeException("Failed to save authentication context", e);
        }
    }
    
    @Override
    public Optional<AuthenticationContext> findByAttemptId(String attemptId) {
        try {
            String key = buildKey(attemptId);
            String jsonValue = redisTemplate.opsForValue().get(key);
            
            if (jsonValue == null) {
                logger.debug("No authentication context found for attempt: {}", attemptId);
                return Optional.empty();
            }
            
            AuthenticationContext context = objectMapper.readValue(jsonValue, AuthenticationContext.class);
            logger.debug("Retrieved authentication context for attempt: {}", attemptId);
            return Optional.of(context);
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing authentication context for attempt: {}", attemptId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void deleteByAttemptId(String attemptId) {
        String key = buildKey(attemptId);
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            logger.debug("Deleted authentication context for attempt: {}", attemptId);
        } else {
            logger.debug("No authentication context found to delete for attempt: {}", attemptId);
        }
    }
    
    @Override
    public boolean existsByAttemptId(String attemptId) {
        String key = buildKey(attemptId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    @Override
    public void update(AuthenticationContext context) {
        update(context, DEFAULT_TTL);
    }
    
    @Override
    public void update(AuthenticationContext context, Duration ttl) {
        // For Redis, update is the same as save since we're storing the entire object
        save(context, ttl);
    }
    
    @Override
    public void deleteExpiredContexts() {
        // Redis automatically handles expiration, but we can clean up any orphaned keys
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                // Check each key to see if it's expired (Redis should handle this automatically)
                int cleanedCount = 0;
                for (String key : keys) {
                    if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                        cleanedCount++;
                    }
                }
                if (cleanedCount > 0) {
                    logger.debug("Found {} expired authentication context keys", cleanedCount);
                }
            }
        } catch (Exception e) {
            logger.warn("Error during expired context cleanup", e);
        }
    }
    
    /**
     * Builds the Redis key for the given attempt ID.
     * 
     * @param attemptId the attempt ID
     * @return the Redis key
     */
    private String buildKey(String attemptId) {
        return KEY_PREFIX + attemptId;
    }
} 