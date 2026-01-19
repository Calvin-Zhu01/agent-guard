package com.agentguard.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 脱敏配置数据模型
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "脱敏配置")
public class MaskConfig {

    /** 需要脱敏的字段类型列表 */
    @Schema(description = "需要脱敏的字段类型列表", example = "[\"phone\", \"idCard\", \"email\"]")
    private List<String> sensitiveFields;

    /** 敏感关键词列表 */
    @Schema(description = "敏感关键词列表", example = "[\"密码\", \"身份证\"]")
    private List<String> sensitiveKeywords;

    /** 自定义脱敏规则，key为字段名 */
    @Schema(description = "自定义脱敏规则，key为字段名")
    private Map<String, MaskRule> maskRules;

    /** URL 过滤模式 */
    @Schema(description = "URL 过滤模式", example = "/api/v1/users/*")
    private String urlPattern;
}
