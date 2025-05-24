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
     * Hashes a PIN using BCrypt.
     * 
     * @param pin the plain text PIN to hash
     * @return the hashed PIN
     */
    public static String hashPin(String pin) {
        if (pin == null) {
            throw new IllegalArgumentException("PIN cannot be null");
        }
        return PASSWORD_ENCODER.encode(pin);
    }
    
    /**
     * Verifies a plain text PIN against a hashed PIN.
     * 
     * @param plainTextPin the plain text PIN provided by the user
     * @param hashedPin the stored hashed PIN
     * @return true if the PIN matches, false otherwise
     */
    public static boolean verifyPin(String plainTextPin, String hashedPin) {
        if (plainTextPin == null || hashedPin == null) {
            return false;
        }
        
        try {
            return PASSWORD_ENCODER.matches(plainTextPin, hashedPin);
        } catch (Exception e) {
            // Log the error but don't expose details for security
            return false;
        }
    }
    
    /**
     * Generates a hash for a given input string.
     * General purpose hashing method.
     * 
     * @param input the input string to hash
     * @return the hashed string
     */
    public static String hash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        return PASSWORD_ENCODER.encode(input);
    }
    
    /**
     * Verifies a plain text input against a hash.
     * General purpose verification method.
     * 
     * @param plainText the plain text input
     * @param hash the stored hash
     * @return true if the input matches the hash, false otherwise
     */
    public static boolean verify(String plainText, String hash) {
        if (plainText == null || hash == null) {
            return false;
        }
        
        try {
            return PASSWORD_ENCODER.matches(plainText, hash);
        } catch (Exception e) {
            return false;
        }
    }
    
    // Private constructor to prevent instantiation
    private EncryptionUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
} 