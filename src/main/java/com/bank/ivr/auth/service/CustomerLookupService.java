package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.repository.CustomerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for looking up customer information from various sources.
 * Supports lookup by SSN from session data and other customer identifiers.
 */
@Service
public class CustomerLookupService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerLookupService.class);
    
    private final CustomerProfileRepository customerRepository;
    
    public CustomerLookupService(CustomerProfileRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    /**
     * Lookup customer by SSN from session data.
     * This method is called when the SSN is available from a previous API call
     * and stored in the session.
     * 
     * @param sessionSsn the SSN retrieved from session
     * @return Optional containing the customer profile if found
     */
    public Optional<CustomerProfile> lookupCustomerBySessionSsn(String sessionSsn) {
        if (sessionSsn == null || sessionSsn.trim().isEmpty()) {
            logger.warn("Session SSN is null or empty, cannot perform lookup");
            return Optional.empty();
        }
        
        // Clean the SSN (remove any formatting)
        String cleanSsn = sessionSsn.replaceAll("[^0-9]", "");
        
        if (cleanSsn.length() != 9) {
            logger.warn("Invalid SSN format in session: expected 9 digits, got {}", cleanSsn.length());
            return Optional.empty();
        }
        
        logger.info("Looking up customer by session SSN: {}***", cleanSsn.substring(0, 3));
        
        Optional<CustomerProfile> customer = customerRepository.findBySsn(cleanSsn);
        
        if (customer.isPresent()) {
            logger.info("Customer found by session SSN: {}", customer.get().getCustomerId());
        } else {
            logger.warn("No customer found for session SSN: {}***", cleanSsn.substring(0, 3));
        }
        
        return customer;
    }
    
    /**
     * Lookup customer by phone number.
     * 
     * @param phoneNumber the customer's phone number
     * @return Optional containing the customer profile if found
     */
    public Optional<CustomerProfile> lookupCustomerByPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.warn("Phone number is null or empty, cannot perform lookup");
            return Optional.empty();
        }
        
        // Clean the phone number (remove any formatting)
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        
        logger.info("Looking up customer by phone: {}***", cleanPhone.substring(0, Math.min(3, cleanPhone.length())));
        
        Optional<CustomerProfile> customer = customerRepository.findByPhoneNumber(phoneNumber);
        
        if (customer.isPresent()) {
            logger.info("Customer found by phone: {}", customer.get().getCustomerId());
        } else {
            logger.warn("No customer found for phone: {}***", cleanPhone.substring(0, Math.min(3, cleanPhone.length())));
        }
        
        return customer;
    }
    
    /**
     * Lookup customer by account number.
     * 
     * @param accountNumber the customer's account number
     * @return Optional containing the customer profile if found
     */
    public Optional<CustomerProfile> lookupCustomerByAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            logger.warn("Account number is null or empty, cannot perform lookup");
            return Optional.empty();
        }
        
        logger.info("Looking up customer by account: {}***", accountNumber.substring(0, Math.min(3, accountNumber.length())));
        
        Optional<CustomerProfile> customer = customerRepository.findByAccountNumber(accountNumber);
        
        if (customer.isPresent()) {
            logger.info("Customer found by account: {}", customer.get().getCustomerId());
        } else {
            logger.warn("No customer found for account: {}***", accountNumber.substring(0, Math.min(3, accountNumber.length())));
        }
        
        return customer;
    }
    
    /**
     * Lookup customer by customer ID.
     * 
     * @param customerId the customer's ID
     * @return Optional containing the customer profile if found
     */
    public Optional<CustomerProfile> lookupCustomerById(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            logger.warn("Customer ID is null or empty, cannot perform lookup");
            return Optional.empty();
        }
        
        logger.info("Looking up customer by ID: {}", customerId);
        
        Optional<CustomerProfile> customer = customerRepository.findByCustomerId(customerId);
        
        if (customer.isPresent()) {
            logger.info("Customer found by ID: {}", customer.get().getCustomerId());
        } else {
            logger.warn("No customer found for ID: {}", customerId);
        }
        
        return customer;
    }
    
    /**
     * Enhanced customer lookup that prioritizes session SSN if available,
     * then falls back to other identifiers.
     * 
     * @param sessionSsn SSN from session (if available)
     * @param phoneNumber phone number identifier
     * @param accountNumber account number identifier
     * @param customerId customer ID identifier
     * @return Optional containing the customer profile if found
     */
    public Optional<CustomerProfile> lookupCustomer(String sessionSsn, String phoneNumber, 
                                                   String accountNumber, String customerId) {
        
        // Priority 1: Session SSN (most reliable)
        if (sessionSsn != null && !sessionSsn.trim().isEmpty()) {
            Optional<CustomerProfile> customer = lookupCustomerBySessionSsn(sessionSsn);
            if (customer.isPresent()) {
                logger.info("Customer lookup successful using session SSN");
                return customer;
            }
        }
        
        // Priority 2: Customer ID
        if (customerId != null && !customerId.trim().isEmpty()) {
            Optional<CustomerProfile> customer = lookupCustomerById(customerId);
            if (customer.isPresent()) {
                logger.info("Customer lookup successful using customer ID");
                return customer;
            }
        }
        
        // Priority 3: Account Number
        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            Optional<CustomerProfile> customer = lookupCustomerByAccount(accountNumber);
            if (customer.isPresent()) {
                logger.info("Customer lookup successful using account number");
                return customer;
            }
        }
        
        // Priority 4: Phone Number
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            Optional<CustomerProfile> customer = lookupCustomerByPhone(phoneNumber);
            if (customer.isPresent()) {
                logger.info("Customer lookup successful using phone number");
                return customer;
            }
        }
        
        logger.warn("Customer lookup failed - no customer found using any provided identifiers");
        return Optional.empty();
    }
} 