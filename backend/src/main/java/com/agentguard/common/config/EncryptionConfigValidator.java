package com.agentguard.common.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 加密配置验证器
 * 确保在生产环境中必须设置加密密钥
 *
 * @author zhuhx
 */
@Slf4j
@Configuration
public class EncryptionConfigValidator {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${agentguard.encryption.key}")
    private String encryptionKey;

    /**
     * 允许使用默认密钥的环境（开发和本地环境）
     */
    private static final List<String> ALLOWED_DEFAULT_KEY_PROFILES = Arrays.asList("dev", "local");

    /**
     * 默认密钥（与 application.yml 中的默认值一致）
     */
    private static final String DEFAULT_KEY = "agentguard-encryption-key-32-bytes-long";

    @PostConstruct
    public void validateEncryptionKey() {
        log.info("验证加密配置，当前环境: {}", activeProfile);

        // 检查密钥是否为空
        if (StrUtil.isBlank(encryptionKey)) {
            throw new IllegalStateException(
                    "加密密钥未配置！请设置环境变量 ENCRYPTION_KEY"
            );
        }

        // 如果不是开发或本地环境，不允许使用默认密钥
        if (!ALLOWED_DEFAULT_KEY_PROFILES.contains(activeProfile)) {
            if (DEFAULT_KEY.equals(encryptionKey)) {
                throw new IllegalStateException(
                        String.format(
                                "生产环境禁止使用默认加密密钥！当前环境: %s，请通过环境变量 ENCRYPTION_KEY 设置安全的加密密钥（建议 32 字节以上）",
                                activeProfile
                        )
                );
            }
        }

        // 验证密钥长度（建议至少 16 字节）
        if (encryptionKey.getBytes().length < 16) {
            log.warn("加密密钥长度过短（当前: {} 字节），建议使用至少 32 字节的密钥以确保安全性",
                    encryptionKey.getBytes().length);
        }

        log.info("加密配置验证通过");
    }
}
