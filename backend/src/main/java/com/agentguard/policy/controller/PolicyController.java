package com.agentguard.policy.controller;

import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.service.AgentPolicyBindingService;
import com.agentguard.common.response.Result;
import com.agentguard.policy.dto.PolicyCreateDTO;
import com.agentguard.policy.dto.PolicyDTO;
import com.agentguard.policy.dto.PolicyUpdateDTO;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import com.agentguard.policy.service.PolicyService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 策略管理控制器
 *
 * @author zhuhx
 */
@Tag(name = "策略管理", description = "策略的增删改查接口")
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final AgentPolicyBindingService policyBindingService;

    @Operation(summary = "创建策略")
    @PostMapping
    public Result<PolicyDTO> create(@Valid @RequestBody PolicyCreateDTO dto) {
        return Result.success(policyService.create(dto));
    }

    @Operation(summary = "获取策略详情")
    @GetMapping("/{id}")
    public Result<PolicyDTO> getById(@PathVariable String id) {
        return Result.success(policyService.getById(id));
    }

    @Operation(summary = "分页查询策略列表")
    @GetMapping
    public Result<IPage<PolicyDTO>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词（名称/描述）") @RequestParam(required = false) String keyword,
            @Parameter(description = "策略类型") @RequestParam(required = false) PolicyType type,
            @Parameter(description = "作用域") @RequestParam(required = false) PolicyScope scope,
            @Parameter(description = "排序方式：priority_desc/priority_asc/updated_desc/updated_asc") @RequestParam(defaultValue = "priority_desc") String sortBy) {
        Page<PolicyDTO> page = new Page<>(current, size);
        return Result.success(policyService.page(page, keyword, type, scope, sortBy));
    }

    @Operation(summary = "更新策略")
    @PutMapping("/{id}")
    public Result<PolicyDTO> update(@PathVariable String id, @Valid @RequestBody PolicyUpdateDTO dto) {
        return Result.success(policyService.update(id, dto));
    }

    @Operation(summary = "删除策略")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        policyService.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用策略")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable String id) {
        policyService.enable(id);
        return Result.success();
    }

    @Operation(summary = "停用策略")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable String id) {
        policyService.disable(id);
        return Result.success();
    }

    @Operation(summary = "获取所有已启用的策略")
    @GetMapping("/enabled")
    public Result<List<PolicyDTO>> getEnabledPolicies() {
        return Result.success(policyService.getEnabledPolicies());
    }

    @Operation(summary = "获取策略绑定的Agent列表")
    @GetMapping("/{policyId}/agents")
    public Result<List<AgentDTO>> getPolicyAgents(@PathVariable String policyId) {
        return Result.success(policyBindingService.getPolicyAgents(policyId));
    }
}
