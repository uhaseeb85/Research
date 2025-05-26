package com.bank.ivr.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class for encryption and decryption operations.
 * Uses BCrypt for secure password/PIN hashing and verification.
 */
public class EncryptionUtil {
    
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    
    /**
     * Generates a hash for a given input string using BCrypt.
     * This is the primary hashing method that should be used for all hashing operations.
     * 
     * @param input the input string to hash (PIN, password, etc.)
     * @return the hashed string
     * @throws IllegalArgumentException if input is null
     */
    public static String hash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        return PASSWORD_ENCODER.encode(input);
    }
    
    /**
     * Verifies a plain text input against a hash using BCrypt.
     * This is the primary verification method that should be used for all verification operations.
     * 
     * @param plainText the plain text input (PIN, password, etc.)
     * @param hashedText the stored hash to verify against
     * @return true if the input matches the hash, false otherwise
     */
    public static boolean verify(String plainText, String hashedText) {
        if (plainText == null || hashedText == null) {
            return false;
        }
        
        try {
            return PASSWORD_ENCODER.matches(plainText, hashedText);
        } catch (Exception e) {
            // Log the error but don't expose details for security
            return false;
        }
    }
    

    
    // Private constructor to prevent instantiation
    private EncryptionUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
} 