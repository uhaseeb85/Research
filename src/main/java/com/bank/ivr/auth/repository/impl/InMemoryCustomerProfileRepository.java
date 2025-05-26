package com.bank.ivr.auth.repository.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.repository.CustomerProfileRepository;
import com.bank.ivr.auth.util.EncryptionUtil;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of CustomerProfileRepository.
 * Stores customer data in Java collections without any database dependency.
 */
@Repository
public class InMemoryCustomerProfileRepository implements CustomerProfileRepository {
    
    private final Map<String, CustomerProfile> customerIdIndex = new HashMap<>();
    private final Map<String, CustomerProfile> phoneNumberIndex = new HashMap<>();
    private final Map<String, CustomerProfile> accountNumberIndex = new HashMap<>();
    
    /**
     * Initialize with sample test data
     */
    @PostConstruct
    public void initializeTestData() {
        // Create test customers
        CustomerProfile customer1 = CustomerProfile.builder()
                .customerId("CUST001")
                .phoneNumber("+1234567890")
                .accountNumber("ACC001")
                .ssn("123456789")
                .hashedPin(EncryptionUtil.hash("1234"))
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .motherMaidenName("Smith")
                .employeeId("EMP001")
                .fullName("John Doe")
                .email("john.doe@email.com")
                .address("123 Main St, Anytown, USA")
                .accountStatus("ACTIVE")
                .build();
        customer1.setId(1L);
        
        CustomerProfile customer2 = CustomerProfile.builder()
                .customerId("CUST002")
                .phoneNumber("+1987654321")
                .accountNumber("ACC002")
                .ssn("987654321")
                .hashedPin(EncryptionUtil.hash("1234"))
                .dateOfBirth(LocalDate.of(1990, 3, 22))
                .motherMaidenName("Johnson")
                .employeeId("EMP002")
                .fullName("Jane Smith")
                .email("jane.smith@email.com")
                .address("456 Oak Ave, Somewhere, USA")
                .accountStatus("ACTIVE")
                .build();
        customer2.setId(2L);
        
        CustomerProfile customer3 = CustomerProfile.builder()
                .customerId("CUST003")
                .phoneNumber("+1555123456")
                .accountNumber("ACC003")
                .ssn("555123456")
                .hashedPin(EncryptionUtil.hash("1234"))
                .dateOfBirth(LocalDate.of(1978, 11, 8))
                .motherMaidenName("Williams")
                .employeeId(null)
                .fullName("Bob Wilson")
                .email("bob.wilson@email.com")
                .address("789 Pine Rd, Elsewhere, USA")
                .accountStatus("ACTIVE")
                .build();
        customer3.setId(3L);
        
        CustomerProfile customer4 = CustomerProfile.builder()
                .customerId("CUST004")
                .phoneNumber("+1444555666")
                .accountNumber("ACC004")
                .ssn(null)
                .hashedPin(null)
                .dateOfBirth(LocalDate.of(1992, 7, 30))
                .motherMaidenName("Brown")
                .employeeId("EMP004")
                .fullName("Alice Brown")
                .email("alice.brown@email.com")
                .address("321 Elm St, Nowhere, USA")
                .accountStatus("INACTIVE")
                .build();
        customer4.setId(4L);
        
        // Store in indexes
        storeCustomer(customer1);
        storeCustomer(customer2);
        storeCustomer(customer3);
        storeCustomer(customer4);
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
    }
    
    @Override
    public Optional<CustomerProfile> findByCustomerId(String customerId) {
        return Optional.ofNullable(customerIdIndex.get(customerId));
    }
    
    @Override
    public Optional<CustomerProfile> findByPhoneNumber(String phoneNumber) {
        return Optional.ofNullable(phoneNumberIndex.get(phoneNumber));
    }
    
    @Override
    public Optional<CustomerProfile> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accountNumberIndex.get(accountNumber));
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
            default:
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
} 