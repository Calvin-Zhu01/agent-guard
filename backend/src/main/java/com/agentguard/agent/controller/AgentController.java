package com.agentguard.agent.controller;

import com.agentguard.agent.dto.AgentCreateDTO;
import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.dto.AgentPolicyBindingDTO;
import com.agentguard.agent.dto.AgentUpdateDTO;
import com.agentguard.agent.service.AgentPolicyBindingService;
import com.agentguard.agent.service.AgentService;
import com.agentguard.common.response.Result;
import com.agentguard.policy.dto.PolicyDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 管理控制器
 *
 * @author zhuhx
 */
@Tag(name = "Agent管理", description = "Agent的增删改查接口")
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentPolicyBindingService policyBindingService;

    @Operation(summary = "创建Agent")
    @PostMapping
    public Result<AgentDTO> create(@Valid @RequestBody AgentCreateDTO dto) {
        return Result.success(agentService.create(dto));
    }

    @Operation(summary = "获取Agent详情")
    @GetMapping("/{id}")
    public Result<AgentDTO> getById(@PathVariable String id) {
        return Result.success(agentService.getById(id));
    }

    @Operation(summary = "分页查询Agent列表")
    @GetMapping
    public Result<IPage<AgentDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<AgentDTO> page = new Page<>(current, size);
        return Result.success(agentService.page(page, keyword));
    }

    @Operation(summary = "更新Agent")
    @PutMapping("/{id}")
    public Result<AgentDTO> update(@PathVariable String id, @Valid @RequestBody AgentUpdateDTO dto) {
        return Result.success(agentService.update(id, dto));
    }

    @Operation(summary = "删除Agent")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        agentService.delete(id);
        return Result.success();
    }

    // ==================== 策略绑定管理 ====================

    @Operation(summary = "绑定策略到Agent")
    @PostMapping("/{agentId}/policies/{policyId}")
    public Result<AgentPolicyBindingDTO> bindPolicy(
            @PathVariable String agentId,
            @PathVariable String policyId) {
        return Result.success(policyBindingService.bindPolicy(agentId, policyId));
    }

    @Operation(summary = "解绑Agent的策略")
    @DeleteMapping("/{agentId}/policies/{policyId}")
    public Result<Void> unbindPolicy(
            @PathVariable String agentId,
            @PathVariable String policyId) {
        policyBindingService.unbindPolicy(agentId, policyId);
        return Result.success();
    }

    @Operation(summary = "获取Agent绑定的策略列表")
    @GetMapping("/{agentId}/policies")
    public Result<List<PolicyDTO>> getAgentPolicies(@PathVariable String agentId) {
        return Result.success(policyBindingService.getAgentPolicies(agentId));
    }

    @Operation(summary = "获取Agent的策略绑定记录")
    @GetMapping("/{agentId}/policy-bindings")
    public Result<List<AgentPolicyBindingDTO>> getAgentPolicyBindings(@PathVariable String agentId) {
        return Result.success(policyBindingService.getBindingsByAgentId(agentId));
    }
}
