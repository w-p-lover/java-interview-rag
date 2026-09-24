package com.javainterview.rag.modelprovider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class ProviderApiKeyCipher {
    private static final int NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public ProviderApiKeyCipher(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("JAVA_INTERVIEW_RAG_PROVIDER_ENCRYPTION_KEY 未配置");
        }
        this.key = new SecretKeySpec(resolveKey(configuredKey.strip()), "AES");
    }

    public EncryptedValue encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Provider API Key 不能为空");
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plainText.strip().getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(Base64.getEncoder().encodeToString(nonce),
                    Base64.getEncoder().encodeToString(ciphertext));
        } catch (Exception exception) {
            throw new IllegalStateException("加密 Provider API Key 失败", exception);
        }
    }

    public String decrypt(String nonceBase64, String ciphertextBase64) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(
                    GCM_TAG_BITS, Base64.getDecoder().decode(nonceBase64)));
            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertextBase64)),
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("解密 Provider API Key 失败，请检查加密主密钥", exception);
        }
    }

    private byte[] resolveKey(String configuredKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // 人类可读密钥使用 SHA-256 派生为固定 256 位密钥。
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("初始化 Provider API Key 加密密钥失败", exception);
        }
    }

    public record EncryptedValue(String nonce, String ciphertext) {
    }
}
