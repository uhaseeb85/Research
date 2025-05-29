package com.bank.ivr.auth.validator.impl;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Validator for Date of Birth tokens.
 * Supports multiple date formats for customer convenience.
 * This is a default implementation that works for all brands.
 */
@Component
public class DateOfBirthValidator implements TokenValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DateOfBirthValidator.class);
    
    private static final DateTimeFormatter[] SUPPORTED_FORMATS = {
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };
    
    @Override
    public String getTokenName() {
        return "DATE_OF_BIRTH";
    }
    
    @Override
    public String getBrand() {
        return "DEFAULT";
    }
    
    @Override
    public boolean validate(String customerIdentifierValue, String providedTokenValue, CustomerProfile customerProfile) {
        if (customerProfile.getDateOfBirth() == null || providedTokenValue == null) {
            logger.debug("Date of birth validation failed: null values");
            return false;
        }
        
        LocalDate providedDate = parseDate(providedTokenValue);
        if (providedDate == null) {
            logger.debug("Date of birth validation failed: invalid date format for customer {}", customerIdentifierValue);
            return false;
        }
        
        LocalDate storedDate = customerProfile.getDateOfBirth();
        boolean isValid = providedDate.equals(storedDate);
        
        logger.debug("Date of birth validation {} for customer {}", 
                    isValid ? "successful" : "failed", customerIdentifierValue);
        
        return isValid;
    }
    
    @Override
    public String normalizeTokenValue(String providedTokenValue) {
        if (providedTokenValue == null) {
            return null;
        }
        // Remove extra spaces and normalize separators
        return providedTokenValue.trim().replaceAll("\\s+", "");
    }
    
    @Override
    public int getPriority() {
        return 80; // Medium priority
    }
    
    /**
     * Attempts to parse the provided date string using multiple supported formats.
     * 
     * @param dateString the date string to parse
     * @return the parsed LocalDate or null if parsing fails
     */
    private LocalDate parseDate(String dateString) {
        String normalized = normalizeTokenValue(dateString);
        
        for (DateTimeFormatter formatter : SUPPORTED_FORMATS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        logger.debug("Failed to parse date string: {}", dateString);
        return null;
    }
} 