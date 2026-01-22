package com.agentguard.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代理服务配置属性
 *
 * @author zhuhx
 */
@Data
@Component
@ConfigurationProperties(prefix = "agentguard.proxy")
public class ProxyProperties {

    /**
     * 是否允许访问内网地址（localhost、私有IP等）
     * 生产环境应设置为 false，开发环境可设置为 true
     */
    private boolean allowInternalAddress = false;
}
