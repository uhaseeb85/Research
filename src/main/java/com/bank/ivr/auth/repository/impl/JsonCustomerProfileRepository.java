package com.bank.ivr.auth.repository.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.repository.CustomerProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON-based implementation of CustomerProfileRepository.
 * Loads customer data from a JSON file instead of a database.
 * This is useful for testing and development environments.
 */
@Repository
@Primary
public class JsonCustomerProfileRepository implements CustomerProfileRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonCustomerProfileRepository.class);
    
    private final Map<String, CustomerProfile> customerIdIndex = new HashMap<>();
    private final Map<String, CustomerProfile> phoneNumberIndex = new HashMap<>();
    private final Map<String, CustomerProfile> accountNumberIndex = new HashMap<>();
    private final Map<String, CustomerProfile> ssnIndex = new HashMap<>();
    
    private final ObjectMapper objectMapper;
    
    public JsonCustomerProfileRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Load customer data from JSON file
     */
    @PostConstruct
    public void loadCustomerData() {
        try {
            ClassPathResource resource = new ClassPathResource("sample-customer-data.json");
            List<CustomerProfile> customers = objectMapper.readValue(
                resource.getInputStream(), 
                new TypeReference<List<CustomerProfile>>() {}
            );
            
            for (CustomerProfile customer : customers) {
                storeCustomer(customer);
            }
            
            logger.info("Loaded {} customers from JSON file", customers.size());
        } catch (IOException e) {
            logger.error("Failed to load customer data from JSON file", e);
            // Fall back to creating some basic test data
            initializeFallbackData();
        }
    }
    
    /**
     * Fallback method to create basic test data if JSON loading fails
     */
    private void initializeFallbackData() {
        logger.warn("Using fallback customer data");
        
        CustomerProfile customer1 = CustomerProfile.builder()
                .customerId("CUST001")
                .phoneNumber("+1234567890")
                .accountNumber("ACC001")
                .ssn("123456789")
                .hashedPin("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .fullName("John Doe")
                .email("john.doe@email.com")
                .accountStatus("ACTIVE")
                .build();
        customer1.setId(1L);
        
        storeCustomer(customer1);
        logger.info("Created fallback customer data with 1 customer");
    }
    
    private void storeCustomer(CustomerProfile customer) {
        if (customer.getCustomerId() != null) {
            customerIdIndex.put(customer.getCustomerId(), customer);
        }
        if (customer.getPhoneNumber() != null) {
            phoneNumberIndex.put(customer.getPhoneNumber(), customer);
        }
        if (customer.getAccountNumber() != null) {
            accountNumberIndex.put(customer.getAccountNumber(), customer);
        }
        if (customer.getSsn() != null) {
            ssnIndex.put(customer.getSsn(), customer);
        }
    }
    
    @Override
    public Optional<CustomerProfile> findByCustomerId(String customerId) {
        CustomerProfile customer = customerIdIndex.get(customerId);
        if (customer != null) {
            logger.debug("Found customer by ID: {}", customerId);
        } else {
            logger.debug("No customer found for ID: {}", customerId);
        }
        return Optional.ofNullable(customer);
    }
    
    @Override
    public Optional<CustomerProfile> findByPhoneNumber(String phoneNumber) {
        CustomerProfile customer = phoneNumberIndex.get(phoneNumber);
        if (customer != null) {
            logger.debug("Found customer by phone: {}", phoneNumber);
        } else {
            logger.debug("No customer found for phone: {}", phoneNumber);
        }
        return Optional.ofNullable(customer);
    }
    
    @Override
    public Optional<CustomerProfile> findByAccountNumber(String accountNumber) {
        CustomerProfile customer = accountNumberIndex.get(accountNumber);
        if (customer != null) {
            logger.debug("Found customer by account: {}", accountNumber);
        } else {
            logger.debug("No customer found for account: {}", accountNumber);
        }
        return Optional.ofNullable(customer);
    }
    
    @Override
    public Optional<CustomerProfile> findBySsn(String ssn) {
        CustomerProfile customer = ssnIndex.get(ssn);
        if (customer != null) {
            logger.debug("Found customer by SSN: {}***", ssn.substring(0, Math.min(3, ssn.length())));
        } else {
            logger.debug("No customer found for SSN: {}***", ssn.substring(0, Math.min(3, ssn.length())));
        }
        return Optional.ofNullable(customer);
    }
    
    @Override
    public Optional<CustomerProfile> findByIdentifier(String identifierType, String identifierValue) {
        switch (identifierType) {
            case "PHONE_NUMBER":
                return findByPhoneNumber(identifierValue);
            case "ACCOUNT_NUMBER":
                return findByAccountNumber(identifierValue);
            case "CUSTOMER_ID":
                return findByCustomerId(identifierValue);
            case "SSN":
                return findBySsn(identifierValue);
            default:
                logger.warn("Unknown identifier type: {}", identifierType);
                return Optional.empty();
        }
    }
    
    @Override
    public boolean existsByCustomerId(String customerId) {
        return customerIdIndex.containsKey(customerId);
    }
    
    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return phoneNumberIndex.containsKey(phoneNumber);
    }
    
    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accountNumberIndex.containsKey(accountNumber);
    }
    
    @Override
    public boolean existsBySsn(String ssn) {
        return ssnIndex.containsKey(ssn);
    }
    
    /**
     * Get all customers (for debugging/testing purposes)
     */
    public Map<String, CustomerProfile> getAllCustomers() {
        return new HashMap<>(customerIdIndex);
    }
    
    /**
     * Get customer count (for debugging/testing purposes)
     */
    public int getCustomerCount() {
        return customerIdIndex.size();
    }
} 