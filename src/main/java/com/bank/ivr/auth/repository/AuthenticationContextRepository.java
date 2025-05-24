package com.bank.ivr.auth.repository;

import com.bank.ivr.auth.model.domain.AuthenticationContext;

import java.time.Duration;
import java.util.Optional;

/**
 * Repository interface for storing and retrieving AuthenticationContext objects.
 * This abstraction allows for different storage implementations (Redis, Database, etc.).
 */
public interface AuthenticationContextRepository {
    
    /**
     * Saves an authentication context with a default TTL.
     * 
     * @param context the authentication context to save
     */
    void save(AuthenticationContext context);
    
    /**
     * Saves an authentication context with a specified TTL.
     * 
     * @param context the authentication context to save
     * @param ttl the time-to-live duration for the context
     */
    void save(AuthenticationContext context, Duration ttl);
    
    /**
     * Retrieves an authentication context by attempt ID.
     * 
     * @param attemptId the attempt ID to search for
     * @return an Optional containing the context if found
     */
    Optional<AuthenticationContext> findByAttemptId(String attemptId);
    
    /**
     * Deletes an authentication context by attempt ID.
     * 
     * @param attemptId the attempt ID of the context to delete
     */
    void deleteByAttemptId(String attemptId);
    
    /**
     * Checks if a context exists for the given attempt ID.
     * 
     * @param attemptId the attempt ID to check
     * @return true if the context exists, false otherwise
     */
    boolean existsByAttemptId(String attemptId);
    
    /**
     * Updates an existing authentication context.
     * 
     * @param context the updated authentication context
     */
    void update(AuthenticationContext context);
    
    /**
     * Updates an existing authentication context with a new TTL.
     * 
     * @param context the updated authentication context
     * @param ttl the new time-to-live duration
     */
    void update(AuthenticationContext context, Duration ttl);
    
    /**
     * Deletes all expired contexts (cleanup operation).
     * Implementation-specific behavior.
     */
    void deleteExpiredContexts();
} 