package com.agentguard.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 代理请求 DTO
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "代理请求")
public class ProxyRequestDTO {

    /** API密钥 */
    @Schema(description = "Agent API密钥", requiredMode = Schema.RequiredMode.REQUIRED, example = "ag_xxx")
    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    /** 目标URL */
    @Schema(description = "目标API URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://api.example.com/transfer")
    @NotBlank(message = "目标URL不能为空")
    private String targetUrl;

    /** HTTP方法 */
    @Schema(description = "HTTP方法", defaultValue = "POST", example = "POST")
    private String method = "POST";

    /** 请求头 */
    @Schema(description = "请求头")
    private Map<String, String> headers;

    /** 请求体 */
    @Schema(description = "请求体")
    private Map<String, Object> body;

    /** 业务元数据（可选） */
    @Schema(description = "业务元数据（可选），用于日志记录和策略评估。可包含 actionType、category、description、riskLevel 等字段", 
            example = "{\"actionType\": \"transfer_funds\", \"category\": \"finance\", \"riskLevel\": \"high\"}")
    private Map<String, Object> metadata;
}
