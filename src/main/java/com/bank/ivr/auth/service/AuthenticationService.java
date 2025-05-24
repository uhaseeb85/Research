package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.request.ProvidedToken;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.repository.AuthenticationContextRepository;
import com.bank.ivr.auth.repository.CustomerProfileRepository;
import com.bank.ivr.auth.rule.AuthenticationRule;
import com.bank.ivr.auth.rule.impl.FullAuthenticationCompletionRule;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main authentication service that orchestrates the IVR authentication flow.
 * Handles both new authentication attempts and continuation of existing attempts.
 */
@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int DEFAULT_OVERALL_ATTEMPTS = 5;
    
    private final AuthenticationContextRepository contextRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final List<AuthenticationRule> rules;
    private final List<TokenValidator> validators;
    private final List<AuthTokenDefinition> tokenDefinitions;
    private final TokenValidationService tokenValidationService;
    
    @Autowired
    public AuthenticationService(
            AuthenticationContextRepository contextRepository,
            CustomerProfileRepository customerProfileRepository,
            List<AuthenticationRule> rules,
            List<TokenValidator> validators,
            List<AuthTokenDefinition> tokenDefinitions,
            TokenValidationService tokenValidationService) {
        this.contextRepository = contextRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.rules = rules;
        this.validators = validators;
        this.tokenDefinitions = tokenDefinitions;
        this.tokenValidationService = tokenValidationService;
    }
    
    /**
     * Main authentication method that handles both new and continuing authentication attempts.
     * 
     * @param request the authentication request
     * @return the authentication response
     */
    public AuthenticationResponse authenticateCustomer(AuthenticationRequest request) {
        logger.info("Processing authentication request for session: {}", request.getSessionId());
        
        try {
            if (request.isNewAttempt()) {
                return handleNewAuthenticationAttempt(request);
            } else {
                return handleContinuingAuthenticationAttempt(request);
            }
        } catch (Exception e) {
            logger.error("Error processing authentication request for session: {}", request.getSessionId(), e);
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("An error occurred during authentication. Please try again.")
                    .build();
        }
    }
    
    /**
     * Handles a new authentication attempt.
     */
    private AuthenticationResponse handleNewAuthenticationAttempt(AuthenticationRequest request) {
        logger.debug("Handling new authentication attempt for session: {}", request.getSessionId());
        
        // Find customer profile
        Optional<CustomerProfile> customerProfileOpt = customerProfileRepository
                .findByCustomerIdentifier(request.getCustomerIdentifier());
        
        if (customerProfileOpt.isEmpty()) {
            logger.warn("Customer not found for identifier: {}", request.getCustomerIdentifier());
            return AuthenticationResponse.builder()
                    .status(AuthStatus.FAILED)
                    .message("Customer not found. Please verify your information.")
                    .build();
        }
        
        CustomerProfile customerProfile = customerProfileOpt.get();
        
        // Generate new attempt ID
        String attemptId = UUID.randomUUID().toString();
        
        // Create authentication context
        AuthenticationContext context = createInitialContext(attemptId, request, customerProfile);
        
        // Save context
        contextRepository.save(context);
        
        // Determine next action
        return buildResponse(context, customerProfile);
    }
    
    /**
     * Handles a continuing authentication attempt.
     */
    private AuthenticationResponse handleContinuingAuthenticationAttempt(AuthenticationRequest request) {
        logger.debug("Handling continuing authentication attempt: {}", request.getAttemptId());
        
        // Retrieve existing context
        Optional<AuthenticationContext> contextOpt = contextRepository.findByAttemptId(request.getAttemptId());
        
        if (contextOpt.isEmpty()) {
            logger.warn("Authentication context not found for attempt: {}", request.getAttemptId());
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("Authentication session expired. Please start over.")
                    .build();
        }
        
        AuthenticationContext context = contextOpt.get();
        
        // Find customer profile
        Optional<CustomerProfile> customerProfileOpt = customerProfileRepository
                .findByCustomerIdentifier(context.getCustomerIdentifier());
        
        if (customerProfileOpt.isEmpty()) {
            logger.error("Customer profile not found for continuing attempt: {}", request.getAttemptId());
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("An error occurred. Please start over.")
                    .build();
        }
        
        CustomerProfile customerProfile = customerProfileOpt.get();
        
        // Process provided tokens
        processProvidedTokens(request, context, customerProfile);
        
        // Save updated context
        contextRepository.update(context);
        
        // Determine next action
        return buildResponse(context, customerProfile);
    }
    
    /**
     * Creates the initial authentication context for a new attempt.
     */
    private AuthenticationContext createInitialContext(String attemptId, AuthenticationRequest request, 
                                                     CustomerProfile customerProfile) {
        // Determine eligible tokens
        List<String> eligibleTokens = determineEligibleTokens(customerProfile);
        
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
                .currentStatus(AuthStatus.PENDING_PRIMARY_TOKEN)
                .failedTokens(new ArrayList<>())
                .build();
    }
    
    /**
     * Determines which tokens a customer is eligible for based on their profile.
     * Uses rule-based evaluation for flexible business logic.
     */
    private List<String> determineEligibleTokens(CustomerProfile customerProfile) {
        List<String> eligibleTokens = new ArrayList<>();
        
        logger.debug("Determining eligible tokens for customer: {}", customerProfile.getCustomerId());
        
        // Create a temporary context for rule evaluation (rules shouldn't need attemptId/sessionId for eligibility)
        // We'll use the rules to check eligibility, but only the customer profile based ones
        for (AuthenticationRule rule : rules) {
            try {
                // Skip the completion rule as it's not an eligibility rule
                if ("FULL_AUTHENTICATION_COMPLETION".equals(rule.getRuleName())) {
                    continue;
                }
                
                // For eligibility rules, we only need customer profile, not full authentication context
                // The current eligibility rules (SSN_ELIGIBILITY, DEBIT_CARD_PIN_ELIGIBILITY) don't actually use context
                boolean isEligible = rule.evaluate(null, customerProfile);
                
                if (isEligible) {
                    String tokenName = mapRuleNameToTokenName(rule.getRuleName());
                    if (tokenName != null) {
                        eligibleTokens.add(tokenName);
                        logger.debug("Customer eligible for {} authentication", tokenName);
                    }
                }
            } catch (Exception e) {
                // Log the error but continue with other rules
                logger.error("Error evaluating rule {}: {}", rule.getRuleName(), e.getMessage());
            }
        }
        
        // Fallback: if no rules worked, use the original hard-coded logic
        if (eligibleTokens.isEmpty()) {
            logger.debug("Rule evaluation failed, falling back to hard-coded eligibility logic");
            
            // Check SSN eligibility
            if (customerProfile.getSsn() != null && !customerProfile.getSsn().trim().isEmpty() 
                && "ACTIVE".equals(customerProfile.getAccountStatus())) {
                eligibleTokens.add("SSN");
                logger.debug("Customer eligible for SSN authentication");
            }
            
            // Check Debit Card PIN eligibility
            if (customerProfile.getHashedPin() != null && !customerProfile.getHashedPin().trim().isEmpty() 
                && "ACTIVE".equals(customerProfile.getAccountStatus())) {
                eligibleTokens.add("DEBIT_CARD_PIN");
                logger.debug("Customer eligible for DEBIT_CARD_PIN authentication");
            }
            
            // Check Date of Birth eligibility
            if (customerProfile.getDateOfBirth() != null && "ACTIVE".equals(customerProfile.getAccountStatus())) {
                eligibleTokens.add("DATE_OF_BIRTH");
                logger.debug("Customer eligible for DATE_OF_BIRTH authentication");
            }
            
            // Check Mother's Maiden Name eligibility
            if (customerProfile.getMotherMaidenName() != null && !customerProfile.getMotherMaidenName().trim().isEmpty() 
                && "ACTIVE".equals(customerProfile.getAccountStatus())) {
                eligibleTokens.add("MOTHER_MAIDEN_NAME");
                logger.debug("Customer eligible for MOTHER_MAIDEN_NAME authentication");
            }
            
            // Check Employee ID eligibility
            if (customerProfile.getEmployeeId() != null && !customerProfile.getEmployeeId().trim().isEmpty() 
                && "ACTIVE".equals(customerProfile.getAccountStatus())) {
                eligibleTokens.add("EMPLOYEE_ID");
                logger.debug("Customer eligible for EMPLOYEE_ID authentication");
            }
        }
        
        logger.debug("Eligible tokens: {}", eligibleTokens);
        return eligibleTokens;
    }
    
    /**
     * Maps rule name to token name for eligibility rules.
     */
    private String mapRuleNameToTokenName(String ruleName) {
        switch (ruleName) {
            case "SSN_ELIGIBILITY":
                return "SSN";
            case "DEBIT_CARD_PIN_ELIGIBILITY":
                return "DEBIT_CARD_PIN";
            case "DATE_OF_BIRTH_ELIGIBILITY":
                return "DATE_OF_BIRTH";
            case "MOTHER_MAIDEN_NAME_ELIGIBILITY":
                return "MOTHER_MAIDEN_NAME";
            case "EMPLOYEE_ID_ELIGIBILITY":
                return "EMPLOYEE_ID";
            default:
                return null;
        }
    }
    
    /**
     * Processes the tokens provided by the customer.
     */
    private void processProvidedTokens(AuthenticationRequest request, AuthenticationContext context, 
                                     CustomerProfile customerProfile) {
        if (request.getProvidedTokens() == null || request.getProvidedTokens().isEmpty()) {
            // No tokens provided - decrement attempts for last asked token
            if (context.getLastAskedToken() != null) {
                context.decrementTokenAttempts(context.getLastAskedToken());
                context.decrementOverallAttempts();
            }
            return;
        }
        
        for (ProvidedToken providedToken : request.getProvidedTokens()) {
            boolean isValid = tokenValidationService.validateToken(
                    providedToken.getTokenName(),
                    context.getCustomerIdentifier().getValue(),
                    providedToken.getTokenValue(),
                    customerProfile
            );
            
            if (isValid) {
                context.addAuthenticatedToken(providedToken.getTokenName());
                logger.debug("Token {} validated successfully for attempt: {}", 
                           providedToken.getTokenName(), context.getAttemptId());
            } else {
                context.decrementTokenAttempts(providedToken.getTokenName());
                context.decrementOverallAttempts();
                
                // Check if this token has failed all attempts
                if (!context.hasRemainingAttemptsForToken(providedToken.getTokenName())) {
                    context.addFailedToken(providedToken.getTokenName());
                }
                
                logger.debug("Token {} validation failed for attempt: {}", 
                           providedToken.getTokenName(), context.getAttemptId());
            }
        }
    }
    
    /**
     * Builds the response based on the current context state.
     */
    private AuthenticationResponse buildResponse(AuthenticationContext context, CustomerProfile customerProfile) {
        // Check for authentication completion
        FullAuthenticationCompletionRule completionRule = new FullAuthenticationCompletionRule();
        if (completionRule.evaluate(context, customerProfile)) {
            context.setCurrentStatus(AuthStatus.AUTHENTICATED);
            contextRepository.deleteByAttemptId(context.getAttemptId());
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.AUTHENTICATED)
                    .message("Authentication successful. Welcome!")
                    .authenticatedTokens(context.getAuthenticatedTokens())
                    .build();
        }
        
        // Check for failure conditions
        if (context.getOverallAttemptsRemaining() <= 0) {
            context.setCurrentStatus(AuthStatus.FAILED);
            contextRepository.deleteByAttemptId(context.getAttemptId());
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("Authentication failed. Too many incorrect attempts.")
                    .build();
        }
        
        // Determine next token to ask
        AuthTokenDefinition nextToken = determineNextToken(context);
        if (nextToken == null) {
            context.setCurrentStatus(AuthStatus.FAILED);
            contextRepository.deleteByAttemptId(context.getAttemptId());
            
            return AuthenticationResponse.builder()
                    .attemptId(context.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("No available authentication methods.")
                    .build();
        }
        
        context.setLastAskedToken(nextToken.getName());
        
        // Determine secondary tokens
        List<AuthTokenDefinition> secondaryTokens = determineSecondaryTokens(context, nextToken);
        
        // Build remaining attempts map
        Map<String, Integer> remainingAttempts = new HashMap<>(context.getTokenAttemptsRemaining());
        remainingAttempts.put("OVERALL", context.getOverallAttemptsRemaining());
        
        return AuthenticationResponse.builder()
                .attemptId(context.getAttemptId())
                .status(context.getAuthenticatedTokens().isEmpty() ? 
                       AuthStatus.PENDING_PRIMARY_TOKEN : AuthStatus.PENDING_MORE_TOKENS)
                .message(buildMessage(nextToken, context))
                .primaryTokenToAsk(nextToken)
                .secondaryTokensAccepted(secondaryTokens)
                .remainingAttempts(remainingAttempts)
                .requiredTokensRemaining(context.getRequiredTokensForFullAuth())
                .authenticatedTokens(context.getAuthenticatedTokens())
                .build();
    }
    
    /**
     * Determines the next token to ask for.
     */
    private AuthTokenDefinition determineNextToken(AuthenticationContext context) {
        return tokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .max(Comparator.comparingInt(AuthTokenDefinition::getPriority))
                .orElse(null);
    }
    
    /**
     * Determines secondary tokens that can be accepted.
     */
    private List<AuthTokenDefinition> determineSecondaryTokens(AuthenticationContext context, 
                                                              AuthTokenDefinition primaryToken) {
        return tokenDefinitions.stream()
                .filter(token -> context.getEligibleTokens().contains(token.getName()))
                .filter(token -> !context.isTokenAuthenticated(token.getName()))
                .filter(token -> !context.isTokenFailed(token.getName()))
                .filter(token -> context.hasRemainingAttemptsForToken(token.getName()))
                .filter(token -> !token.getName().equals(primaryToken.getName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Builds an appropriate message for the customer.
     */
    private String buildMessage(AuthTokenDefinition nextToken, AuthenticationContext context) {
        if (context.getAuthenticatedTokens().isEmpty()) {
            return "Please provide your " + nextToken.getDescription() + ".";
        } else {
            return "Thank you. Now please provide your " + nextToken.getDescription() + ".";
        }
    }
} 