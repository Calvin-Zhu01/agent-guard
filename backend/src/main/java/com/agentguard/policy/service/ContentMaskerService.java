package com.agentguard.policy.service;

import com.agentguard.policy.dto.MaskConfig;

import java.util.List;

/**
 * 内容脱敏服务接口
 *
 * @author zhuhx
 */
public interface ContentMaskerService {

    /**
     * 对响应内容进行脱敏处理
     *
     * @param content 原始内容（Map 或 String）
     * @param config 脱敏配置
     * @return 脱敏后的内容
     */
    Object maskContent(Object content, MaskConfig config);

    /**
     * 对指定字段进行脱敏
     *
     * @param fieldType 字段类型（phone/idCard/bankCard/email/name/address）
     * @param value 原始值
     * @return 脱敏后的值
     */
    String maskField(String fieldType, String value);

    /**
     * 替换敏感关键词
     *
     * @param text 原始文本
     * @param keywords 敏感关键词列表
     * @return 替换后的文本
     */
    String maskKeywords(String text, List<String> keywords);
}
