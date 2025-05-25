package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.BrandFailurePolicy;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for evaluating brand-specific failure policies and determining
 * when to fail authentication vs. ask for alternative tokens.
 */
@Service
public class BrandFailurePolicyService {
    
    private static final Logger logger = LoggerFactory.getLogger(BrandFailurePolicyService.class);
    
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public BrandFailurePolicyService(BrandAuthConfigurationService brandConfigService) {
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Evaluates if authentication should fail immediately based on brand failure policy.
     * 
     * @param context the authentication context
     * @param customerProfile the customer profile
     * @param brand the brand code
     * @return true if authentication should fail immediately
     */
    public boolean shouldFailAuthentication(AuthenticationContext context, CustomerProfile customerProfile, String brand) {
        BrandFailurePolicy failurePolicy = brandConfigService.getBrandFailurePolicy(brand);
        
        logger.debug("Evaluating failure policy for brand: {}, strategy: {}, failed tokens: {}, authenticated tokens: {}", 
                    brand, failurePolicy.getFailureStrategy(), context.getFailedTokens(), context.getAuthenticatedTokens());
        
        // Check if policy indicates immediate failure
        boolean shouldFail = failurePolicy.shouldFailImmediately(context);
        
        if (shouldFail) {
            logger.info("Brand failure policy indicates immediate failure for brand: {}, attempt: {}, reason: {}", 
                       brand, context.getAttemptId(), getFailureReason(failurePolicy, context));
        }
        
        return shouldFail;
    }
    
    /**
     * Gets alternative tokens based on brand failure policy.
     * 
     * @param context the authentication context
     * @param brand the brand code
     * @param brandTokenDefinitions available token definitions
     * @return list of alternative token definitions
     */
    public List<AuthTokenDefinition> getAlternativeTokens(AuthenticationContext context, String brand, 
                                                          List<AuthTokenDefinition> brandTokenDefinitions) {
        BrandFailurePolicy failurePolicy = brandConfigService.getBrandFailurePolicy(brand);
        
        logger.debug("Getting alternative tokens for brand: {}, strategy: {}, failed tokens: {}", 
                    brand, failurePolicy.getAlternativeTokenStrategy(), context.getFailedTokens());
        
        List<AuthTokenDefinition> alternatives = new ArrayList<>();
        
        switch (failurePolicy.getAlternativeTokenStrategy()) {
            case ANY_REMAINING:
                alternatives = getAnyRemainingTokens(context, brandTokenDefinitions);
                break;
                
            case PREDEFINED_ALTERNATIVES:
                alternatives = getPredefinedAlternatives(context, failurePolicy, brandTokenDefinitions);
                break;
                
            case PRIORITY_BASED:
                alternatives = getPriorityBasedAlternatives(context, brandTokenDefinitions);
                break;
                
            case GROUP_BASED:
                alternatives = getGroupBasedAlternatives(context, failurePolicy, brandTokenDefinitions);
                break;
                
            default:
                logger.warn("Unknown alternative token strategy: {} for brand: {}", 
                           failurePolicy.getAlternativeTokenStrategy(), brand);
                alternatives = getPriorityBasedAlternatives(context, brandTokenDefinitions);
        }
        
        logger.debug("Found {} alternative tokens for brand: {}: {}", 
                    alternatives.size(), brand, 
                    alternatives.stream().map(AuthTokenDefinition::getName).collect(Collectors.toList()));
        
        return alternatives;
    }
    
    /**
     * Gets the next best alternative token based on brand failure policy.
     * 
     * @param context the authentication context
     * @param brand the brand code
     * @param brandTokenDefinitions available token definitions
     * @return the best alternative token or null if none available
     */
    public AuthTokenDefinition getNextAlternativeToken(AuthenticationContext context, String brand, 
                                                       List<AuthTokenDefinition> brandTokenDefinitions) {
        List<AuthTokenDefinition> alternatives = getAlternativeTokens(context, brand, brandTokenDefinitions);
        
        if (alternatives.isEmpty()) {
            logger.debug("No alternative tokens available for brand: {}, attempt: {}", brand, context.getAttemptId());
            return null;
        }
        
        // Return the highest priority alternative
        AuthTokenDefinition bestAlternative = alternatives.get(0);
        for (AuthTokenDefinition token : alternatives) {
            if (token.getPriority() > bestAlternative.getPriority()) {
                bestAlternative = token;
            }
        }
        
        logger.debug("Selected alternative token: {} (priority: {}) for brand: {}", 
                    bestAlternative.getName(), bestAlternative.getPriority(), brand);
        
        return bestAlternative;
    }
    
    /**
     * Checks if partial authentication is allowed based on brand policy.
     * 
     * @param context the authentication context
     * @param brand the brand code
     * @return true if partial authentication is allowed
     */
    public boolean isPartialAuthenticationAllowed(AuthenticationContext context, String brand) {
        BrandFailurePolicy failurePolicy = brandConfigService.getBrandFailurePolicy(brand);
        
        if (!failurePolicy.isAllowPartialAuthentication()) {
            return false;
        }
        
        int authenticatedCount = context.getAuthenticatedTokens().size();
        boolean allowed = authenticatedCount >= failurePolicy.getPartialAuthMinTokens();
        
        logger.debug("Partial authentication check for brand: {}, authenticated: {}, min required: {}, allowed: {}", 
                    brand, authenticatedCount, failurePolicy.getPartialAuthMinTokens(), allowed);
        
        return allowed;
    }
    
    /**
     * Gets any remaining eligible tokens.
     */
    private List<AuthTokenDefinition> getAnyRemainingTokens(AuthenticationContext context, 
                                                            List<AuthTokenDefinition> brandTokenDefinitions) {
        return brandTokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .filter(token -> context.canReAskToken(token.getName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets predefined alternative tokens based on failure policy configuration.
     */
    private List<AuthTokenDefinition> getPredefinedAlternatives(AuthenticationContext context, 
                                                               BrandFailurePolicy failurePolicy,
                                                               List<AuthTokenDefinition> brandTokenDefinitions) {
        List<AuthTokenDefinition> alternatives = new ArrayList<>();
        
        for (String failedToken : context.getFailedTokens()) {
            List<String> alternativeNames = failurePolicy.getAlternativesForToken(failedToken);
            for (String altName : alternativeNames) {
                brandTokenDefinitions.stream()
                        .filter(token -> token.getName().equals(altName))
                        .filter(token -> context.getEligibleTokens().contains(token.getName()))
                        .filter(token -> !context.isTokenAuthenticated(token.getName()))
                        .filter(token -> !context.isTokenFailed(token.getName()))
                        .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                        .filter(token -> context.canReAskToken(token.getName()))
                        .findFirst()
                        .ifPresent(alternatives::add);
            }
        }
        
        return alternatives;
    }
    
    /**
     * Gets priority-based alternative tokens.
     */
    private List<AuthTokenDefinition> getPriorityBasedAlternatives(AuthenticationContext context, 
                                                                  List<AuthTokenDefinition> brandTokenDefinitions) {
        return brandTokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .filter(token -> context.canReAskToken(token.getName()))
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority())) // Highest priority first
                .collect(Collectors.toList());
    }
    
    /**
     * Gets group-based alternative tokens.
     */
    private List<AuthTokenDefinition> getGroupBasedAlternatives(AuthenticationContext context, 
                                                               BrandFailurePolicy failurePolicy,
                                                               List<AuthTokenDefinition> brandTokenDefinitions) {
        List<AuthTokenDefinition> alternatives = new ArrayList<>();
        
        // Try fallback groups in order
        for (String groupName : failurePolicy.getFallbackGroups()) {
            List<String> groupTokens = failurePolicy.getTokensInGroup(groupName);
            
            for (String tokenName : groupTokens) {
                brandTokenDefinitions.stream()
                        .filter(token -> token.getName().equals(tokenName))
                        .filter(token -> context.getEligibleTokens().contains(token.getName()))
                        .filter(token -> !context.isTokenAuthenticated(token.getName()))
                        .filter(token -> !context.isTokenFailed(token.getName()))
                        .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                        .filter(token -> context.canReAskToken(token.getName()))
                        .findFirst()
                        .ifPresent(alternatives::add);
            }
            
            // If we found tokens in this group, use them and stop
            if (!alternatives.isEmpty()) {
                break;
            }
        }
        
        return alternatives;
    }
    
    /**
     * Gets a human-readable failure reason.
     */
    private String getFailureReason(BrandFailurePolicy failurePolicy, AuthenticationContext context) {
        switch (failurePolicy.getFailureStrategy()) {
            case FAIL_IMMEDIATELY:
                return "Immediate failure on any token failure";
            case ALLOW_ALTERNATIVES:
                return "All alternatives exhausted";
            case REQUIRE_ALL_ATTEMPTED:
                return "All eligible tokens attempted";
            case PROGRESSIVE_FALLBACK:
                return "All fallback groups exhausted";
            default:
                return "Unknown failure strategy";
        }
    }
} 