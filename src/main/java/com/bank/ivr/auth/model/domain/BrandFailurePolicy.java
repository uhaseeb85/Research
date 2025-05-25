package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defines brand-specific policies for handling authentication failures.
 * Determines when to send final authentication failure vs. asking for alternative tokens.
 */
public class BrandFailurePolicy {
    
    public enum FailureStrategy {
        FAIL_IMMEDIATELY,           // Fail as soon as any required token fails
        ALLOW_ALTERNATIVES,         // Try alternative tokens when required tokens fail
        REQUIRE_ALL_ATTEMPTED,      // Only fail after all eligible tokens have been tried
        PROGRESSIVE_FALLBACK        // Use fallback token groups in order
    }
    
    public enum AlternativeTokenStrategy {
        ANY_REMAINING,              // Any remaining eligible token can be used
        PREDEFINED_ALTERNATIVES,    // Only specific alternative tokens allowed
        PRIORITY_BASED,             // Use next highest priority token
        GROUP_BASED                 // Use alternative token groups
    }
    
    @JsonProperty("brandCode")
    private final String brandCode;
    
    @JsonProperty("failureStrategy")
    private final FailureStrategy failureStrategy;
    
    @JsonProperty("alternativeTokenStrategy")
    private final AlternativeTokenStrategy alternativeTokenStrategy;
    
    @JsonProperty("requiredTokenFailureThreshold")
    private final int requiredTokenFailureThreshold;
    
    @JsonProperty("maxAlternativeAttempts")
    private final int maxAlternativeAttempts;
    
    @JsonProperty("tokenAlternatives")
    private final Map<String, List<String>> tokenAlternatives;
    
    @JsonProperty("tokenGroups")
    private final Map<String, List<String>> tokenGroups;
    
    @JsonProperty("fallbackGroups")
    private final List<String> fallbackGroups;
    
    @JsonProperty("criticalTokens")
    private final List<String> criticalTokens;
    
    @JsonProperty("allowPartialAuthentication")
    private final boolean allowPartialAuthentication;
    
    @JsonProperty("partialAuthMinTokens")
    private final int partialAuthMinTokens;
    
    @JsonProperty("failOnCriticalTokenFailure")
    private final boolean failOnCriticalTokenFailure;
    
    @JsonProperty("enableGracefulDegradation")
    private final boolean enableGracefulDegradation;
    
    @JsonProperty("degradationThreshold")
    private final int degradationThreshold;
    
    @JsonCreator
    public BrandFailurePolicy(
            @JsonProperty("brandCode") String brandCode,
            @JsonProperty("failureStrategy") FailureStrategy failureStrategy,
            @JsonProperty("alternativeTokenStrategy") AlternativeTokenStrategy alternativeTokenStrategy,
            @JsonProperty("requiredTokenFailureThreshold") int requiredTokenFailureThreshold,
            @JsonProperty("maxAlternativeAttempts") int maxAlternativeAttempts,
            @JsonProperty("tokenAlternatives") Map<String, List<String>> tokenAlternatives,
            @JsonProperty("tokenGroups") Map<String, List<String>> tokenGroups,
            @JsonProperty("fallbackGroups") List<String> fallbackGroups,
            @JsonProperty("criticalTokens") List<String> criticalTokens,
            @JsonProperty("allowPartialAuthentication") boolean allowPartialAuthentication,
            @JsonProperty("partialAuthMinTokens") int partialAuthMinTokens,
            @JsonProperty("failOnCriticalTokenFailure") boolean failOnCriticalTokenFailure,
            @JsonProperty("enableGracefulDegradation") boolean enableGracefulDegradation,
            @JsonProperty("degradationThreshold") int degradationThreshold) {
        this.brandCode = brandCode;
        this.failureStrategy = failureStrategy;
        this.alternativeTokenStrategy = alternativeTokenStrategy;
        this.requiredTokenFailureThreshold = requiredTokenFailureThreshold;
        this.maxAlternativeAttempts = maxAlternativeAttempts;
        this.tokenAlternatives = tokenAlternatives;
        this.tokenGroups = tokenGroups;
        this.fallbackGroups = fallbackGroups;
        this.criticalTokens = criticalTokens;
        this.allowPartialAuthentication = allowPartialAuthentication;
        this.partialAuthMinTokens = partialAuthMinTokens;
        this.failOnCriticalTokenFailure = failOnCriticalTokenFailure;
        this.enableGracefulDegradation = enableGracefulDegradation;
        this.degradationThreshold = degradationThreshold;
    }
    
    // Getters
    public String getBrandCode() { return brandCode; }
    public FailureStrategy getFailureStrategy() { return failureStrategy; }
    public AlternativeTokenStrategy getAlternativeTokenStrategy() { return alternativeTokenStrategy; }
    public int getRequiredTokenFailureThreshold() { return requiredTokenFailureThreshold; }
    public int getMaxAlternativeAttempts() { return maxAlternativeAttempts; }
    public Map<String, List<String>> getTokenAlternatives() { return tokenAlternatives; }
    public Map<String, List<String>> getTokenGroups() { return tokenGroups; }
    public List<String> getFallbackGroups() { return fallbackGroups; }
    public List<String> getCriticalTokens() { return criticalTokens; }
    public boolean isAllowPartialAuthentication() { return allowPartialAuthentication; }
    public int getPartialAuthMinTokens() { return partialAuthMinTokens; }
    public boolean isFailOnCriticalTokenFailure() { return failOnCriticalTokenFailure; }
    public boolean isEnableGracefulDegradation() { return enableGracefulDegradation; }
    public int getDegradationThreshold() { return degradationThreshold; }
    
    /**
     * Determines if authentication should fail immediately based on current context.
     */
    public boolean shouldFailImmediately(AuthenticationContext context) {
        // Check critical token failures
        if (failOnCriticalTokenFailure) {
            for (String criticalToken : criticalTokens) {
                if (context.isTokenFailed(criticalToken)) {
                    return true;
                }
            }
        }
        
        // Check failure strategy
        switch (failureStrategy) {
            case FAIL_IMMEDIATELY:
                return !context.getFailedTokens().isEmpty();
                
            case ALLOW_ALTERNATIVES:
                return hasExhaustedAllAlternatives(context);
                
            case REQUIRE_ALL_ATTEMPTED:
                return hasAttemptedAllEligibleTokens(context);
                
            case PROGRESSIVE_FALLBACK:
                return hasExhaustedAllFallbackGroups(context);
                
            default:
                return false;
        }
    }
    
    /**
     * Gets alternative tokens for a failed token.
     */
    public List<String> getAlternativesForToken(String failedToken) {
        return tokenAlternatives.getOrDefault(failedToken, List.of());
    }
    
    /**
     * Gets tokens in a specific group.
     */
    public List<String> getTokensInGroup(String groupName) {
        return tokenGroups.getOrDefault(groupName, List.of());
    }
    
    /**
     * Checks if all alternatives for failed tokens have been exhausted.
     */
    private boolean hasExhaustedAllAlternatives(AuthenticationContext context) {
        for (String failedToken : context.getFailedTokens()) {
            List<String> alternatives = getAlternativesForToken(failedToken);
            for (String alternative : alternatives) {
                if (!context.isTokenFailed(alternative) && 
                    !context.isTokenAuthenticated(alternative) &&
                    context.hasRemainingAttemptsForToken(alternative)) {
                    return false; // Still have alternatives
                }
            }
        }
        return true; // All alternatives exhausted
    }
    
    /**
     * Checks if all eligible tokens have been attempted.
     */
    private boolean hasAttemptedAllEligibleTokens(AuthenticationContext context) {
        for (String eligibleToken : context.getEligibleTokens()) {
            if (!context.isTokenFailed(eligibleToken) && 
                !context.isTokenAuthenticated(eligibleToken) &&
                context.hasRemainingAttemptsForToken(eligibleToken)) {
                return false; // Still have tokens to try
            }
        }
        return true; // All tokens attempted
    }
    
    /**
     * Checks if all fallback groups have been exhausted.
     */
    private boolean hasExhaustedAllFallbackGroups(AuthenticationContext context) {
        for (String groupName : fallbackGroups) {
            List<String> groupTokens = getTokensInGroup(groupName);
            for (String token : groupTokens) {
                if (context.getEligibleTokens().contains(token) &&
                    !context.isTokenFailed(token) && 
                    !context.isTokenAuthenticated(token) &&
                    context.hasRemainingAttemptsForToken(token)) {
                    return false; // Still have tokens in this group
                }
            }
        }
        return true; // All fallback groups exhausted
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String brandCode;
        private FailureStrategy failureStrategy = FailureStrategy.ALLOW_ALTERNATIVES;
        private AlternativeTokenStrategy alternativeTokenStrategy = AlternativeTokenStrategy.PRIORITY_BASED;
        private int requiredTokenFailureThreshold = 1;
        private int maxAlternativeAttempts = 3;
        private Map<String, List<String>> tokenAlternatives = Map.of();
        private Map<String, List<String>> tokenGroups = Map.of();
        private List<String> fallbackGroups = List.of();
        private List<String> criticalTokens = List.of();
        private boolean allowPartialAuthentication = false;
        private int partialAuthMinTokens = 1;
        private boolean failOnCriticalTokenFailure = true;
        private boolean enableGracefulDegradation = false;
        private int degradationThreshold = 2;
        
        public Builder brandCode(String brandCode) {
            this.brandCode = brandCode;
            return this;
        }
        
        public Builder failureStrategy(FailureStrategy failureStrategy) {
            this.failureStrategy = failureStrategy;
            return this;
        }
        
        public Builder alternativeTokenStrategy(AlternativeTokenStrategy alternativeTokenStrategy) {
            this.alternativeTokenStrategy = alternativeTokenStrategy;
            return this;
        }
        
        public Builder requiredTokenFailureThreshold(int requiredTokenFailureThreshold) {
            this.requiredTokenFailureThreshold = requiredTokenFailureThreshold;
            return this;
        }
        
        public Builder maxAlternativeAttempts(int maxAlternativeAttempts) {
            this.maxAlternativeAttempts = maxAlternativeAttempts;
            return this;
        }
        
        public Builder tokenAlternatives(Map<String, List<String>> tokenAlternatives) {
            this.tokenAlternatives = tokenAlternatives;
            return this;
        }
        
        public Builder tokenGroups(Map<String, List<String>> tokenGroups) {
            this.tokenGroups = tokenGroups;
            return this;
        }
        
        public Builder fallbackGroups(List<String> fallbackGroups) {
            this.fallbackGroups = fallbackGroups;
            return this;
        }
        
        public Builder criticalTokens(List<String> criticalTokens) {
            this.criticalTokens = criticalTokens;
            return this;
        }
        
        public Builder allowPartialAuthentication(boolean allowPartialAuthentication) {
            this.allowPartialAuthentication = allowPartialAuthentication;
            return this;
        }
        
        public Builder partialAuthMinTokens(int partialAuthMinTokens) {
            this.partialAuthMinTokens = partialAuthMinTokens;
            return this;
        }
        
        public Builder failOnCriticalTokenFailure(boolean failOnCriticalTokenFailure) {
            this.failOnCriticalTokenFailure = failOnCriticalTokenFailure;
            return this;
        }
        
        public Builder enableGracefulDegradation(boolean enableGracefulDegradation) {
            this.enableGracefulDegradation = enableGracefulDegradation;
            return this;
        }
        
        public Builder degradationThreshold(int degradationThreshold) {
            this.degradationThreshold = degradationThreshold;
            return this;
        }
        
        public BrandFailurePolicy build() {
            return new BrandFailurePolicy(
                brandCode, failureStrategy, alternativeTokenStrategy, requiredTokenFailureThreshold,
                maxAlternativeAttempts, tokenAlternatives, tokenGroups, fallbackGroups,
                criticalTokens, allowPartialAuthentication, partialAuthMinTokens,
                failOnCriticalTokenFailure, enableGracefulDegradation, degradationThreshold
            );
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrandFailurePolicy that = (BrandFailurePolicy) o;
        return Objects.equals(brandCode, that.brandCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(brandCode);
    }
    
    @Override
    public String toString() {
        return "BrandFailurePolicy{" +
                "brandCode='" + brandCode + '\'' +
                ", failureStrategy=" + failureStrategy +
                ", alternativeTokenStrategy=" + alternativeTokenStrategy +
                '}';
    }
} 