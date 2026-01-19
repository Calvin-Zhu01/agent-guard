package com.agentguard.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义脱敏规则
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "自定义脱敏规则")
public class MaskRule {

    /** 保留开头字符数 */
    @Schema(description = "保留开头字符数", example = "3")
    private int start;

    /** 保留结尾字符数 */
    @Schema(description = "保留结尾字符数", example = "4")
    private int end;

    /** 替换字符 */
    @Schema(description = "替换字符", example = "*", defaultValue = "*")
    @Builder.Default
    private String maskChar = "*";
}
