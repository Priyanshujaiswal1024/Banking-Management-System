package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    // Generates hash in format: salt:hash (both Base64)
    public static String hash(String plainText) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedBytes = md.digest(plainText.getBytes());

            return Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    // Verifies plainText against stored "salt:hash"
    public static boolean verify(String plainText, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] originalHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] testHash = md.digest(plainText.getBytes());

            return MessageDigest.isEqual(originalHash, testHash);
        } catch (Exception e) {
            return false;
        }
    }
}