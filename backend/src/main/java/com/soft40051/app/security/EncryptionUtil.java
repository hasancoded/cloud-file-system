package com.soft40051.app.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Encryption Utility for File Storage
 * Implements AES-128 encryption for data at rest
 * 
 * Security Note:
 * This is a simplified implementation for academic demonstration.
 * Production systems should use:
 * - Key management systems (KMS)
 * - Per-file encryption keys
 * - IV (Initialization Vector) for each encryption
 * - HMAC for authentication
 * 
 * @author SOFT40051 Submission
 * @version 1.0
 */
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // Static key for demonstration (INSECURE - use KMS in production)
    private static final byte[] KEY_BYTES = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    
    private static final SecretKey SECRET_KEY = new SecretKeySpec(KEY_BYTES, ALGORITHM);
    
    /**
     * Encrypt string content
     * @param plaintext Original content
     * @return Base64-encoded ciphertext
     */
    public static String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
        
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * Decrypt ciphertext
     * @param ciphertext Base64-encoded encrypted content
     * @return Original plaintext
     */
    public static String decrypt(String ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY);
        
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decryptedBytes, "UTF-8");
    }
    
    /**
     * Generate new AES key (for demonstration)
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(128);
        return keyGen.generateKey();
    }
}