package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.AuthenticationSession;
import com.bank.ivr.auth.model.domain.AttemptState;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.TokenState;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.repository.AuthenticationContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for managing brand-aware authentication context creation and lifecycle.
 */
@Service
public class AuthenticationContextService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationContextService.class);
    
    private final AuthenticationContextRepository contextRepository;
    private final EligibilityService eligibilityService;
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public AuthenticationContextService(
            AuthenticationContextRepository contextRepository,
            EligibilityService eligibilityService,
            BrandAuthConfigurationService brandConfigService) {
        this.contextRepository = contextRepository;
        this.eligibilityService = eligibilityService;
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Creates the initial authentication context for a new attempt with full brand awareness.
     */
    public AuthenticationContext createInitialContext(String attemptId, AuthenticationRequest request, 
                                                     CustomerProfile customerProfile) {
        String brand = request.getBrand();
        logger.debug("Creating brand-aware initial context for attempt: {}, brand: {}", attemptId, brand);
        
        // Determine eligible tokens with brand awareness
        List<String> eligibleTokens = eligibilityService.determineEligibleTokens(customerProfile, brand);
        
        // Get brand-specific token definitions for attempt calculations
        List<AuthTokenDefinition> brandTokenDefinitions = brandConfigService.getTokenDefinitionsForBrand(brand);
        Map<String, Integer> brandSpecificAttempts = brandConfigService.getBrandSpecificTokenAttempts(brand);
        
        // Initialize token attempts using brand-specific configurations
        Map<String, Integer> tokenAttempts = new HashMap<>();
        for (AuthTokenDefinition tokenDef : brandTokenDefinitions) {
            if (eligibleTokens.contains(tokenDef.getName())) {
                // Use brand-specific override if available, otherwise use token definition default
                Integer attempts = brandSpecificAttempts.get(tokenDef.getName());
                if (attempts == null) {
                    attempts = tokenDef.getMaxAttempts();
                }
                tokenAttempts.put(tokenDef.getName(), attempts);
            }
        }
        
        // Get brand-specific overall attempts limit
        int maxOverallAttempts = brandConfigService.getMaxOverallAttemptsForBrand(brand);
        
        logger.debug("Brand-aware context created - Brand: {}, EligibleTokens: {}, MaxOverallAttempts: {}", 
                    brand, eligibleTokens, maxOverallAttempts);
        
        // Create session information
        AuthenticationSession session = AuthenticationSession.builder()
                .attemptId(attemptId)
                .sessionId(request.getSessionId())
                .customerIdentifier(request.getCustomerIdentifier())
                .brand(brand)
                .startTime(LocalDateTime.now())
                .build();
        
        // Create token state
        TokenState tokenState = TokenState.builder()
                .eligibleTokens(eligibleTokens)
                .authenticatedTokens(new ArrayList<>())

                .failedTokens(new ArrayList<>())
                .askedTokens(new ArrayList<>())
                .build();
        
        // Create attempt state
        AttemptState attemptState = AttemptState.builder()
                .tokenAttemptsRemaining(tokenAttempts)
                .overallAttemptsRemaining(maxOverallAttempts)
                .currentStatus(com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus.PENDING_PRIMARY_TOKEN)
                .build();
        
        return AuthenticationContext.builder()
                .session(session)
                .tokenState(tokenState)
                .attemptState(attemptState)
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