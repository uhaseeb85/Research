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
 * This service delegates specific responsibilities to specialized services and is fully brand-aware.
 */
@Service
public class AuthenticationOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationOrchestrator.class);
    
    private final CustomerProfileRepository customerProfileRepository;
    private final AuthenticationContextService contextService;
    private final TokenProcessingService tokenProcessingService;
    private final AuthenticationResponseService responseService;
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public AuthenticationOrchestrator(
            CustomerProfileRepository customerProfileRepository,
            AuthenticationContextService contextService,
            TokenProcessingService tokenProcessingService,
            AuthenticationResponseService responseService,
            BrandAuthConfigurationService brandConfigService) {
        this.customerProfileRepository = customerProfileRepository;
        this.contextService = contextService;
        this.tokenProcessingService = tokenProcessingService;
        this.responseService = responseService;
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Main authentication method that handles both new and continuing authentication attempts.
     * Now fully brand-aware across the entire authentication flow.
     * 
     * @param request the authentication request containing brand information
     * @return the authentication response
     */
    public AuthenticationResponse authenticateCustomer(AuthenticationRequest request) {
        String brand = request.getBrand();
        logger.info("Processing brand-aware authentication request for session: {}, brand: {}", 
                   request.getSessionId(), brand);
        
        try {
            // Validate brand support before processing
            if (!brandConfigService.isBrandSupported(brand)) {
                logger.warn("Unsupported brand '{}' in authentication request for session: {}", 
                           brand, request.getSessionId());
                return AuthenticationResponse.builder()
                        .attemptId(request.getAttemptId())
                        .status(AuthStatus.FAILED)
                        .message("Brand '" + brand + "' is not supported")
                        .build();
            }
            
            if (request.isNewAttempt()) {
                return handleNewAuthenticationAttempt(request);
            } else {
                return handleContinuingAuthenticationAttempt(request);
            }
        } catch (Exception e) {
            logger.error("Error processing brand-aware authentication request for session: {}, brand: {}", 
                        request.getSessionId(), brand, e);
            
            // Use brand-specific error message
            String errorMessage = brandConfigService.getBrandMessage(brand, "failure");
            if (errorMessage == null) {
                errorMessage = "An error occurred during authentication. Please try again.";
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(errorMessage)
                    .build();
        }
    }
    
    /**
     * Handles a new authentication attempt with full brand awareness.
     */
    private AuthenticationResponse handleNewAuthenticationAttempt(AuthenticationRequest request) {
        String brand = request.getBrand();
        logger.debug("Handling new brand-aware authentication attempt for session: {}, brand: {}", 
                    request.getSessionId(), brand);
        
        // Find customer profile
        Optional<CustomerProfile> customerProfileOpt = customerProfileRepository
                .findByCustomerIdentifier(request.getCustomerIdentifier());
        
        if (customerProfileOpt.isEmpty()) {
            logger.warn("Customer not found for identifier: {}, brand: {}", 
                       request.getCustomerIdentifier(), brand);
            
            // Use brand-specific message for customer not found
            String errorMessage = brandConfigService.getBrandMessage(brand, "customer_not_found");
            if (errorMessage == null) {
                errorMessage = "Customer not found. Please verify your information.";
            }
            
            return AuthenticationResponse.builder()
                    .status(AuthStatus.FAILED)
                    .message(errorMessage)
                    .build();
        }
        
        CustomerProfile customerProfile = customerProfileOpt.get();
        
        // Generate new attempt ID
        String attemptId = contextService.generateAttemptId();
        
        // Create brand-aware authentication context
        AuthenticationContext context = contextService.createInitialContext(attemptId, request, customerProfile);
        
        // Save context
        contextService.saveContext(context);
        
        // Build and return brand-aware response
        return responseService.buildResponse(context, customerProfile, brand);
    }
    
    /**
     * Handles a continuing authentication attempt with brand awareness.
     */
    private AuthenticationResponse handleContinuingAuthenticationAttempt(AuthenticationRequest request) {
        String brand = request.getBrand();
        logger.debug("Handling continuing brand-aware authentication attempt: {}, brand: {}", 
                    request.getAttemptId(), brand);
        
        // Retrieve existing context
        Optional<AuthenticationContext> contextOpt = contextService.getContextByAttemptId(request.getAttemptId());
        
        if (contextOpt.isEmpty()) {
            logger.warn("Authentication context not found for attempt: {}, brand: {}", 
                       request.getAttemptId(), brand);
            
            // Use brand-specific message for expired session
            String errorMessage = brandConfigService.getBrandMessage(brand, "session_expired");
            if (errorMessage == null) {
                errorMessage = "Authentication session expired. Please start over.";
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(errorMessage)
                    .build();
        }
        
        AuthenticationContext context = contextOpt.get();
        
        // Validate brand consistency
        if (!brand.equals(context.getBrand())) {
            logger.warn("Brand mismatch in continuing attempt. Request brand: {}, Context brand: {}, AttemptId: {}", 
                       brand, context.getBrand(), request.getAttemptId());
            
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("Brand mismatch detected. Please start over.")
                    .build();
        }
        
        // Find customer profile
        Optional<CustomerProfile> customerProfileOpt = customerProfileRepository
                .findByCustomerIdentifier(context.getCustomerIdentifier());
        
        if (customerProfileOpt.isEmpty()) {
            logger.error("Customer profile not found for continuing attempt: {}, brand: {}", 
                        request.getAttemptId(), brand);
            
            String errorMessage = brandConfigService.getBrandMessage(brand, "system_error");
            if (errorMessage == null) {
                errorMessage = "An error occurred. Please start over.";
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(errorMessage)
                    .build();
        }
        
        CustomerProfile customerProfile = customerProfileOpt.get();
        
        // Process provided tokens with brand awareness
        tokenProcessingService.processProvidedTokens(request, context, customerProfile);
        
        // Save updated context
        contextService.updateContext(context);
        
        // Build and return brand-aware response
        return responseService.buildResponse(context, customerProfile, brand);
    }
} 