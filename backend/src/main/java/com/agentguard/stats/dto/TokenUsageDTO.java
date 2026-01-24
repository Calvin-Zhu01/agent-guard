package com.agentguard.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Token 使用情况 DTO
 *
 * @author zhuhx
 */
@Data
@Builder
@Schema(description = "Token使用情况")
public class TokenUsageDTO {

    @Schema(description = "输入Token数")
    private Integer inputTokens;

    @Schema(description = "输出Token数")
    private Integer outputTokens;

    @Schema(description = "总Token数")
    private Integer totalTokens;

    @Schema(description = "是否为估算值")
    private Boolean estimated;

    @Schema(description = "解析来源：response/tiktoken/character")
    private String source;
}
