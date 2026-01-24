package com.agentguard.stats.token;

import com.agentguard.stats.dto.TokenUsageDTO;

/**
 * Token 解析器接口
 *
 * @author zhuhx
 */
public interface TokenParser {

    /**
     * 从响应中解析 Token 使用情况
     *
     * @param responseBody 响应体（JSON字符串）
     * @param provider LLM提供商
     * @return Token使用情况
     */
    TokenUsageDTO parseFromResponse(String responseBody, String provider);

    /**
     * 估算请求的 Token 数
     *
     * @param text 文本内容
     * @param model 模型名称
     * @return 估算的Token数
     */
    int estimateTokens(String text, String model);
}
