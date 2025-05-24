package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.repository.AuthenticationContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for managing authentication context creation and lifecycle.
 */
@Service
public class AuthenticationContextService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationContextService.class);
    private static final int DEFAULT_OVERALL_ATTEMPTS = 5;
    
    private final AuthenticationContextRepository contextRepository;
    private final EligibilityService eligibilityService;
    private final List<AuthTokenDefinition> tokenDefinitions;
    
    @Autowired
    public AuthenticationContextService(
            AuthenticationContextRepository contextRepository,
            EligibilityService eligibilityService,
            List<AuthTokenDefinition> tokenDefinitions) {
        this.contextRepository = contextRepository;
        this.eligibilityService = eligibilityService;
        this.tokenDefinitions = tokenDefinitions;
    }
    
    /**
     * Creates the initial authentication context for a new attempt.
     */
    public AuthenticationContext createInitialContext(String attemptId, AuthenticationRequest request, 
                                                     CustomerProfile customerProfile) {
        // Determine eligible tokens
        List<String> eligibleTokens = eligibilityService.determineEligibleTokens(customerProfile);
        
        // Set required tokens (business rule: require at least one primary authentication factor)
        List<String> requiredTokens = Arrays.asList("SSN"); // Simplified - in reality this would be configurable
        
        // Initialize token attempts
        Map<String, Integer> tokenAttempts = new HashMap<>();
        for (AuthTokenDefinition tokenDef : tokenDefinitions) {
            if (eligibleTokens.contains(tokenDef.getName())) {
                tokenAttempts.put(tokenDef.getName(), tokenDef.getMaxAttempts());
            }
        }
        
        return AuthenticationContext.builder()
                .attemptId(attemptId)
                .sessionId(request.getSessionId())
                .customerIdentifier(request.getCustomerIdentifier())
                .startTime(LocalDateTime.now())
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(DEFAULT_OVERALL_ATTEMPTS)
                .eligibleTokens(eligibleTokens)
                .authenticatedTokens(new ArrayList<>())
                .requiredTokensForFullAuth(new ArrayList<>(requiredTokens))
                .currentStatus(com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus.PENDING_PRIMARY_TOKEN)
                .failedTokens(new ArrayList<>())
                .build();
    }
    
    /**
     * Retrieves an authentication context by attempt ID.
     */
    public Optional<AuthenticationContext> getContextByAttemptId(String attemptId) {
        return contextRepository.findByAttemptId(attemptId);
    }
    
    /**
     * Saves or updates an authentication context.
     */
    public void saveContext(AuthenticationContext context) {
        contextRepository.save(context);
    }
    
    /**
     * Updates an existing authentication context.
     */
    public void updateContext(AuthenticationContext context) {
        contextRepository.update(context);
    }
    
    /**
     * Deletes an authentication context by attempt ID.
     */
    public void deleteContext(String attemptId) {
        contextRepository.deleteByAttemptId(attemptId);
    }
    
    /**
     * Generates a new unique attempt ID.
     */
    public String generateAttemptId() {
        return UUID.randomUUID().toString();
    }
} 