package com.bank.ivr.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for repository implementations.
 * Controls which customer repository implementation to use based on application properties.
 */
@Configuration
public class RepositoryConfiguration {
    
    /**
     * Use JSON-based customer repository by default.
     * This can be overridden by setting the property:
     * ivr.auth.repository.customer.type=memory
     */
    @Primary
    @ConditionalOnProperty(
        name = "ivr.auth.repository.customer.type", 
        havingValue = "json", 
        matchIfMissing = true
    )
    public static class JsonRepositoryConfig {
        // JsonCustomerProfileRepository will be used as primary
    }
    
    /**
     * Use in-memory customer repository when explicitly configured.
     * Set property: ivr.auth.repository.customer.type=memory
     */
    @ConditionalOnProperty(
        name = "ivr.auth.repository.customer.type", 
        havingValue = "memory"
    )
    public static class InMemoryRepositoryConfig {
        // InMemoryCustomerProfileRepository will be used
    }
} 