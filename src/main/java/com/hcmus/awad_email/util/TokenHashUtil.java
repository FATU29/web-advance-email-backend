package com.hcmus.awad_email.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for hashing tokens before storage.
 * Uses SHA-256 to create a one-way hash of tokens.
 * This ensures that even if the database is compromised,
 * the actual tokens cannot be recovered.
 */
public class TokenHashUtil {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Hashes a token using SHA-256 and returns a Base64-encoded string.
     * This is a one-way operation - the original token cannot be recovered.
     *
     * @param token The token to hash
     * @return Base64-encoded SHA-256 hash of the token
     */
    public static String hashToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all Java implementations
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies if a plain token matches a stored hash.
     *
     * @param plainToken The plain token to verify
     * @param storedHash The stored hash to compare against
     * @return true if the token matches the hash, false otherwise
     */
    public static boolean verifyToken(String plainToken, String storedHash) {
        if (plainToken == null || storedHash == null) {
            return false;
        }
        String hashedToken = hashToken(plainToken);
        return hashedToken.equals(storedHash);
    }
}

