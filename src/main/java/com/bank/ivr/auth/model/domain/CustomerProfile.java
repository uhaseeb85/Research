package com.bank.ivr.auth.model.domain;

import java.time.LocalDate;

public class CustomerProfile {
    
    private Long id;
    private String customerId;
    private String phoneNumber;
    private String accountNumber;
    private String ssn;
    private String hashedPin;
    private LocalDate dateOfBirth;
    private String motherMaidenName;
    private String employeeId;
    private String fullName;
    private String email;
    private String address;
    private String accountStatus;
    
    // Default constructor
    public CustomerProfile() {}
    
    // Builder constructor
    private CustomerProfile(Builder builder) {
        this.customerId = builder.customerId;
        this.phoneNumber = builder.phoneNumber;
        this.accountNumber = builder.accountNumber;
        this.ssn = builder.ssn;
        this.hashedPin = builder.hashedPin;
        this.dateOfBirth = builder.dateOfBirth;
        this.motherMaidenName = builder.motherMaidenName;
        this.employeeId = builder.employeeId;
        this.fullName = builder.fullName;
        this.email = builder.email;
        this.address = builder.address;
        this.accountStatus = builder.accountStatus;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getSsn() {
        return ssn;
    }
    
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }
    
    public String getHashedPin() {
        return hashedPin;
    }
    
    public void setHashedPin(String hashedPin) {
        this.hashedPin = hashedPin;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getMotherMaidenName() {
        return motherMaidenName;
    }
    
    public void setMotherMaidenName(String motherMaidenName) {
        this.motherMaidenName = motherMaidenName;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String customerId;
        private String phoneNumber;
        private String accountNumber;
        private String ssn;
        private String hashedPin;
        private LocalDate dateOfBirth;
        private String motherMaidenName;
        private String employeeId;
        private String fullName;
        private String email;
        private String address;
        private String accountStatus = "ACTIVE"; // default
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }
        
        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }
        
        public Builder ssn(String ssn) {
            this.ssn = ssn;
            return this;
        }
        
        public Builder hashedPin(String hashedPin) {
            this.hashedPin = hashedPin;
            return this;
        }
        
        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }
        
        public Builder motherMaidenName(String motherMaidenName) {
            this.motherMaidenName = motherMaidenName;
            return this;
        }
        
        public Builder employeeId(String employeeId) {
            this.employeeId = employeeId;
            return this;
        }
        
        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder accountStatus(String accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }
        
        public CustomerProfile build() {
            if (customerId == null) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            return new CustomerProfile(this);
        }
    }
    
    @Override
    public String toString() {
        return "CustomerProfile{" +
               "id=" + id +
               ", customerId='" + customerId + '\'' +
               ", phoneNumber='" + phoneNumber + '\'' +
               ", accountNumber='" + accountNumber + '\'' +
               ", ssn='[MASKED]'" +
               ", hashedPin='[MASKED]'" +
               ", dateOfBirth=" + dateOfBirth +
               ", motherMaidenName='[MASKED]'" +
               ", employeeId='" + employeeId + '\'' +
               ", fullName='" + fullName + '\'' +
               ", email='" + email + '\'' +
               ", address='" + address + '\'' +
               ", accountStatus='" + accountStatus + '\'' +
               '}';
    }
} 