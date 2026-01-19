package com.agentguard.approval.dto;

import com.agentguard.approval.enums.ExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批执行结果 DTO
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "审批执行结果")
public class ApprovalExecutionResultDTO {

    @Schema(description = "审批请求ID")
    private String approvalId;

    @Schema(description = "执行状态")
    private ExecutionStatus status;

    @Schema(description = "执行结果消息")
    private String message;

    @Schema(description = "执行响应数据")
    private Object response;

    /**
     * 创建成功结果
     *
     * @param approvalId 审批请求ID
     * @param response 响应数据
     * @return 成功的执行结果
     */
    public static ApprovalExecutionResultDTO success(String approvalId, Object response) {
        return ApprovalExecutionResultDTO.builder()
                .approvalId(approvalId)
                .status(ExecutionStatus.SUCCESS)
                .message("执行成功")
                .response(response)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param approvalId 审批请求ID
     * @param message 失败消息
     * @return 失败的执行结果
     */
    public static ApprovalExecutionResultDTO failed(String approvalId, String message) {
        return ApprovalExecutionResultDTO.builder()
                .approvalId(approvalId)
                .status(ExecutionStatus.FAILED)
                .message(message)
                .response(null)
                .build();
    }
}
