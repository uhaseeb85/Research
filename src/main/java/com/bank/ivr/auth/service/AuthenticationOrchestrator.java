package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.repository.CustomerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Main authentication orchestrator that coordinates the IVR authentication flow.
 * This service delegates specific responsibilities to specialized services.
 */
@Service
public class AuthenticationOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationOrchestrator.class);
    
    private final CustomerProfileRepository customerProfileRepository;
    private final AuthenticationContextService contextService;
    private final TokenProcessingService tokenProcessingService;
    private final AuthenticationResponseService responseService;
    
    @Autowired
    public AuthenticationOrchestrator(
            CustomerProfileRepository customerProfileRepository,
            AuthenticationContextService contextService,
            TokenProcessingService tokenProcessingService,
            AuthenticationResponseService responseService) {
        this.customerProfileRepository = customerProfileRepository;
        this.contextService = contextService;
        this.tokenProcessingService = tokenProcessingService;
        this.responseService = responseService;
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
        String attemptId = contextService.generateAttemptId();
        
        // Create authentication context
        AuthenticationContext context = contextService.createInitialContext(attemptId, request, customerProfile);
        
        // Save context
        contextService.saveContext(context);
        
        // Build and return response
        return responseService.buildResponse(context, customerProfile);
    }
    
    /**
     * Handles a continuing authentication attempt.
     */
    private AuthenticationResponse handleContinuingAuthenticationAttempt(AuthenticationRequest request) {
        logger.debug("Handling continuing authentication attempt: {}", request.getAttemptId());
        
        // Retrieve existing context
        Optional<AuthenticationContext> contextOpt = contextService.getContextByAttemptId(request.getAttemptId());
        
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
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);
        
        // Save updated context
        contextService.updateContext(context);
        
        // Build and return response
        return responseService.buildResponse(context, customerProfile);
    }
} 