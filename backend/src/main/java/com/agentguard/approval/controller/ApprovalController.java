package com.agentguard.approval.controller;

import com.agentguard.approval.dto.ApprovalActionDTO;
import com.agentguard.approval.dto.ApprovalDTO;
import com.agentguard.approval.dto.ApprovalStatusDTO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.service.ApprovalService;
import com.agentguard.common.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审批管理控制器
 *
 * @author zhuhx
 */
@Tag(name = "审批管理", description = "审批请求的查询和处理接口")
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "分页查询审批列表")
    @GetMapping
    public Result<IPage<ApprovalDTO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "审批状态") @RequestParam(required = false) ApprovalStatus status,
            @Parameter(description = "Agent ID") @RequestParam(required = false) String agentId) {
        Page<ApprovalDTO> page = new Page<>(current, size);
        return Result.success(approvalService.page(page, status, agentId));
    }

    @Operation(summary = "获取审批详情")
    @GetMapping("/{id}")
    public Result<ApprovalDTO> getById(@PathVariable String id) {
        return Result.success(approvalService.getById(id));
    }

    @Operation(summary = "批准审批请求")
    @PostMapping("/{id}/approve")
    public Result<ApprovalDTO> approve(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionDTO actionDTO) {
        String approverId = actionDTO != null ? actionDTO.getApproverId() : null;
        String remark = actionDTO != null ? actionDTO.getRemark() : null;
        return Result.success(approvalService.approve(id, approverId, remark));
    }

    @Operation(summary = "拒绝审批请求")
    @PostMapping("/{id}/reject")
    public Result<ApprovalDTO> reject(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionDTO actionDTO) {
        String approverId = actionDTO != null ? actionDTO.getApproverId() : null;
        String remark = actionDTO != null ? actionDTO.getRemark() : null;
        return Result.success(approvalService.reject(id, approverId, remark));
    }

    @Operation(summary = "获取待审批数量")
    @GetMapping("/pending/count")
    public Result<Long> getPendingCount() {
        return Result.success(approvalService.getPendingCount());
    }

    @Operation(
            summary = "查询审批状态（用于客户端SDK轮询）",
            description = "返回审批状态，当审批通过时返回执行结果，当审批拒绝时返回拒绝原因"
    )
    @GetMapping("/{id}/status")
    public Result<ApprovalStatusDTO> getStatus(@PathVariable String id) {
        return Result.success(approvalService.getStatus(id));
    }
}
