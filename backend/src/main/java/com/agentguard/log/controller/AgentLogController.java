package com.agentguard.log.controller;

import com.agentguard.common.response.Result;
import com.agentguard.log.dto.AgentLogDTO;
import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.agentguard.log.service.AgentLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent日志控制器
 *
 * @author zhuhx
 */
@Tag(name = "Agent日志", description = "Agent行为日志查询接口")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class AgentLogController {

    private final AgentLogService agentLogService;

    @Operation(summary = "分页查询日志列表")
    @GetMapping
    public Result<IPage<AgentLogDTO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Agent ID过滤") @RequestParam(required = false) String agentId,
            @Parameter(description = "响应状态过滤") @RequestParam(required = false) ResponseStatus responseStatus,
            @Parameter(description = "请求类型过滤") @RequestParam(required = false) RequestType requestType) {
        Page<AgentLogDTO> page = new Page<>(current, size);
        return Result.success(agentLogService.page(page, agentId, responseStatus, requestType));
    }

    @Operation(summary = "获取日志详情")
    @GetMapping("/{id}")
    public Result<AgentLogDTO> getById(@Parameter(description = "日志ID") @PathVariable String id) {
        return Result.success(agentLogService.getById(id));
    }
}
