package com.agentguard.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 * 用于敏感数据的加密和解密（如 LLM API Key）
 *
 * @author zhuhx
 */
@Slf4j
@Component
public class EncryptionUtil {

    private final AES aes;

    public EncryptionUtil(@Value("${agentguard.encryption.key}") String encryptionKey) {
        // 确保密钥长度为 32 字节（256 位）
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 如果密钥不足 32 字节，填充到 32 字节
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            this.aes = new AES("ECB", "PKCS5Padding", paddedKey);
        } else {
            // 如果密钥超过 32 字节，截取前 32 字节
            byte[] truncatedKey = new byte[32];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
            this.aes = new AES("ECB", "PKCS5Padding", truncatedKey);
        }
        log.info("EncryptionUtil initialized with AES-256-ECB");
    }

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return 加密后的 Base64 字符串，如果输入为空则返回 null
     */
    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return null;
        }
        try {
            return aes.encryptBase64(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param encryptedText 加密后的 Base64 字符串
     * @return 解密后的明文，如果输入为空则返回 null
     */
    public String decrypt(String encryptedText) {
        if (StrUtil.isBlank(encryptedText)) {
            return null;
        }
        try {
            return aes.decryptStr(encryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
