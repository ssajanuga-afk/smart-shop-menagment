package com.example.smartshopmanager.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic and input helpers for staff PIN authentication.
 */
public final class AuthUtils {

    private AuthUtils() {}

    /**
     * Hashes a raw PIN string with SHA-256 and returns lowercase hex.
     */
    public static String hashPin(String rawPin) {
        if (rawPin == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawPin.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Verifies a candidate raw PIN against a stored SHA-256 hex digest in constant time.
     */
    public static boolean verifyPin(String rawPin, String storedHash) {
        if (rawPin == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        String candidate = hashPin(rawPin);
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Validates that a staff PIN has 4 to 8 digits.
     */
    public static boolean isValidPin(String rawPin) {
        if (rawPin == null) return false;
        String trimmed = rawPin.trim();
        if (trimmed.length() < 4 || trimmed.length() > 8) return false;
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
