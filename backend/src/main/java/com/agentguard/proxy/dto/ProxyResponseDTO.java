package com.agentguard.proxy.dto;

import com.agentguard.log.enums.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理响应 DTO
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "代理响应")
public class ProxyResponseDTO {

    /** 响应状态 */
    @Schema(description = "响应状态：SUCCESS/FAILED/BLOCKED/PENDING_APPROVAL", example = "SUCCESS")
    private ResponseStatus status;

    /** HTTP 状态码 */
    @Schema(description = "HTTP 状态码")
    private Integer statusCode;

    /** 消息 */
    @Schema(description = "响应消息", example = "请求成功")
    private String message;

    /** 响应数据 */
    @Schema(description = "响应数据")
    private Object response;

    /** 审批请求ID（当状态为PENDING_APPROVAL时返回） */
    @Schema(description = "审批请求ID")
    private String approvalRequestId;

    /**
     * 创建成功响应
     *
     * @param response 响应数据
     * @return 成功的ProxyResponseDTO
     */
    public static ProxyResponseDTO success(Object response) {
        return ProxyResponseDTO.builder()
                .status(ResponseStatus.SUCCESS)
                .message("请求成功")
                .response(response)
                .build();
    }

    /**
     * 创建失败响应
     *
     * @param message 失败消息
     * @return 失败的ProxyResponseDTO
     */
    public static ProxyResponseDTO failed(String message) {
        return ProxyResponseDTO.builder()
                .status(ResponseStatus.FAILED)
                .message(message)
                .response(null)
                .build();
    }

    /**
     * 创建被拦截响应
     *
     * @param reason 拦截原因
     * @return 被拦截的ProxyResponseDTO
     */
    public static ProxyResponseDTO blocked(String reason) {
        return ProxyResponseDTO.builder()
                .status(ResponseStatus.BLOCKED)
                .message(reason)
                .response(null)
                .build();
    }

    /**
     * 创建待审批响应
     *
     * @param reason 审批原因
     * @param approvalRequestId 审批请求ID
     * @return 待审批的ProxyResponseDTO
     */
    public static ProxyResponseDTO pendingApproval(String reason, String approvalRequestId) {
        return ProxyResponseDTO.builder()
                .status(ResponseStatus.PENDING_APPROVAL)
                .message(reason)
                .response(null)
                .approvalRequestId(approvalRequestId)
                .build();
    }
}
