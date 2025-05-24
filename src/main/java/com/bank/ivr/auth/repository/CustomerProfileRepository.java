package com.bank.ivr.auth.repository;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.request.CustomerIdentifier;

import java.util.Optional;

/**
 * Repository interface for CustomerProfile entity.
 * Provides methods to find customers by various identifier types.
 */
public interface CustomerProfileRepository {
    
    /**
     * Finds a customer by their customer ID.
     * 
     * @param customerId the customer ID
     * @return an Optional containing the customer profile if found
     */
    Optional<CustomerProfile> findByCustomerId(String customerId);
    
    /**
     * Finds a customer by their phone number.
     * 
     * @param phoneNumber the phone number
     * @return an Optional containing the customer profile if found
     */
    Optional<CustomerProfile> findByPhoneNumber(String phoneNumber);
    
    /**
     * Finds a customer by their account number.
     * 
     * @param accountNumber the account number
     * @return an Optional containing the customer profile if found
     */
    Optional<CustomerProfile> findByAccountNumber(String accountNumber);
    
    /**
     * Finds a customer by the provided identifier based on the identifier type.
     * 
     * @param identifierType the type of identifier (PHONE_NUMBER, ACCOUNT_NUMBER, CUSTOMER_ID)
     * @param identifierValue the identifier value
     * @return an Optional containing the customer profile if found
     */
    Optional<CustomerProfile> findByIdentifier(String identifierType, String identifierValue);
    
    /**
     * Checks if a customer exists by their customer ID.
     * 
     * @param customerId the customer ID
     * @return true if the customer exists, false otherwise
     */
    boolean existsByCustomerId(String customerId);
    
    /**
     * Checks if a customer exists by their phone number.
     * 
     * @param phoneNumber the phone number
     * @return true if the customer exists, false otherwise
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * Checks if a customer exists by their account number.
     * 
     * @param accountNumber the account number
     * @return true if the customer exists, false otherwise
     */
    boolean existsByAccountNumber(String accountNumber);
    
    /**
     * Finds a customer using the CustomerIdentifier object.
     * 
     * @param customerIdentifier the customer identifier containing type and value
     * @return an Optional containing the customer profile if found
     */
    default Optional<CustomerProfile> findByCustomerIdentifier(CustomerIdentifier customerIdentifier) {
        return findByIdentifier(customerIdentifier.getType().name(), customerIdentifier.getValue());
    }
} 