package com.agentguard.stats.token.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agentguard.stats.dto.TokenUsageDTO;
import com.agentguard.stats.token.TokenParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Token 解析器实现类
 *
 * @author zhuhx
 */
@Slf4j
@Component
public class TokenParserImpl implements TokenParser {

    @Override
    public TokenUsageDTO parseFromResponse(String responseBody, String provider) {
        if (responseBody == null || responseBody.isEmpty()) {
            log.warn("响应体为空，无法解析Token使用量");
            return null;
        }

        if (provider == null || provider.isEmpty()) {
            log.warn("提供商为空，使用默认值 'openai'");
            provider = "openai";
        }

        try {
            JSONObject json = JSONUtil.parseObj(responseBody);
            log.debug("正在解析提供商 {} 的Token使用量", provider);

            // 根据不同提供商解析
            switch (provider.toLowerCase()) {
                case "openai":
                case "azure":
                    return parseOpenAIFormat(json);
                case "anthropic":
                    return parseAnthropicFormat(json);
                default:
                    log.warn("未知的提供商: {}，尝试使用OpenAI格式解析", provider);
                    return parseOpenAIFormat(json);
            }
        } catch (Exception e) {
            log.error("解析Token使用量失败: {}", e.getMessage(), e);
            log.debug("解析失败的响应体: {}", responseBody);
            return null;
        }
    }

    /**
     * 解析 OpenAI 格式的响应
     * {
     *   "usage": {
     *     "prompt_tokens": 10,
     *     "completion_tokens": 20,
     *     "total_tokens": 30
     *   }
     * }
     */
    private TokenUsageDTO parseOpenAIFormat(JSONObject json) {
        JSONObject usage = json.getJSONObject("usage");
        if (usage == null) {
            log.warn("OpenAI格式响应中未找到 'usage' 字段");
            return null;
        }

        Integer promptTokens = usage.getInt("prompt_tokens");
        Integer completionTokens = usage.getInt("completion_tokens");
        Integer totalTokens = usage.getInt("total_tokens");

        if (promptTokens == null || completionTokens == null) {
            log.warn("usage对象中缺少Token字段: prompt_tokens={}, completion_tokens={}",
                    promptTokens, completionTokens);
            return null;
        }

        log.debug("解析OpenAI格式Token: prompt={}, completion={}, total={}",
                promptTokens, completionTokens, totalTokens);

        return TokenUsageDTO.builder()
                .inputTokens(promptTokens)
                .outputTokens(completionTokens)
                .totalTokens(totalTokens != null ? totalTokens : (promptTokens + completionTokens))
                .estimated(false)
                .source("response")
                .build();
    }

    /**
     * 解析 Anthropic 格式的响应
     * {
     *   "usage": {
     *     "input_tokens": 10,
     *     "output_tokens": 20
     *   }
     * }
     */
    private TokenUsageDTO parseAnthropicFormat(JSONObject json) {
        JSONObject usage = json.getJSONObject("usage");
        if (usage == null) {
            log.warn("Anthropic格式响应中未找到 'usage' 字段");
            return null;
        }

        Integer inputTokens = usage.getInt("input_tokens");
        Integer outputTokens = usage.getInt("output_tokens");

        if (inputTokens == null || outputTokens == null) {
            log.warn("usage对象中缺少Token字段: input_tokens={}, output_tokens={}",
                    inputTokens, outputTokens);
            return null;
        }

        log.debug("解析Anthropic格式Token: input={}, output={}, total={}",
                inputTokens, outputTokens, inputTokens + outputTokens);

        return TokenUsageDTO.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens)
                .estimated(false)
                .source("response")
                .build();
    }

    @Override
    public int estimateTokens(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 简单估算：中英文混合，平均每2.5个字符约等于1个token
        // 这是一个粗略估算，实际应该使用 tiktoken 等专业库
        return (int) Math.ceil(text.length() / 2.5);
    }
}
