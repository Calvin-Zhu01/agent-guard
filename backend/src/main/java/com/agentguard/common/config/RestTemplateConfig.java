package com.agentguard.common.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 * 配置 HTTP 客户端用于代理请求转发
 *
 * @author zhuhx
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 配置 RestTemplate Bean
     * - 连接超时：5 秒
     * - 读取超时：30 秒
     * - 连接池：最大连接数 100，每个路由最大连接数 50
     */
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = createRequestFactory();
        return new RestTemplate(factory);
    }

    /**
     * 创建 HttpComponentsClientHttpRequestFactory
     * 配置 Apache HttpClient 5.x 连接池
     */
    private HttpComponentsClientHttpRequestFactory createRequestFactory() {
        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(50);

        // 配置请求参数（使用 HttpClient 5.x API）
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        // 创建 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 创建请求工厂
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
