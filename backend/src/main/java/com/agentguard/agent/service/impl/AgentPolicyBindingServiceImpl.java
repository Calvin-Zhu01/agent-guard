package com.agentguard.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.dto.AgentPolicyBindingDTO;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.entity.AgentPolicyBindingDO;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.agent.mapper.AgentPolicyBindingMapper;
import com.agentguard.agent.service.AgentPolicyBindingService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.policy.dto.PolicyDTO;
import com.agentguard.policy.entity.PolicyDO;
import com.agentguard.policy.mapper.PolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent策略绑定服务实现类
 *
 * @author zhuhx
 */
@Service
@RequiredArgsConstructor
public class AgentPolicyBindingServiceImpl implements AgentPolicyBindingService {

    private final AgentPolicyBindingMapper bindingMapper;
    private final AgentMapper agentMapper;
    private final PolicyMapper policyMapper;

    @Override
    @Transactional
    public AgentPolicyBindingDTO bindPolicy(String agentId, String policyId) {
        // 验证Agent存在
        AgentDO agent = agentMapper.selectById(agentId);
        if (ObjectUtil.isNull(agent)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        // 验证策略存在
        PolicyDO policy = policyMapper.selectById(policyId);
        if (ObjectUtil.isNull(policy)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }

        // 检查绑定是否已存在
        if (isBindingExists(agentId, policyId)) {
            throw new BusinessException(ErrorCode.POLICY_BINDING_ALREADY_EXISTS);
        }

        // 创建绑定
        AgentPolicyBindingDO binding = new AgentPolicyBindingDO();
        binding.setAgentId(agentId);
        binding.setPolicyId(policyId);
        bindingMapper.insert(binding);

        // 返回绑定信息
        return toDTO(binding, agent.getName(), policy.getName());
    }

    @Override
    @Transactional
    public void unbindPolicy(String agentId, String policyId) {
        LambdaQueryWrapper<AgentPolicyBindingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentPolicyBindingDO::getAgentId, agentId)
               .eq(AgentPolicyBindingDO::getPolicyId, policyId);

        int deleted = bindingMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.POLICY_BINDING_NOT_FOUND);
        }
    }

    @Override
    public List<PolicyDTO> getAgentPolicies(String agentId) {
        // 验证Agent存在
        AgentDO agent = agentMapper.selectById(agentId);
        if (ObjectUtil.isNull(agent)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        // 获取绑定的策略ID列表
        List<String> policyIds = bindingMapper.selectPolicyIdsByAgentId(agentId);
        if (CollUtil.isEmpty(policyIds)) {
            return new ArrayList<>();
        }

        // 查询策略详情
        List<PolicyDO> policies = policyMapper.selectBatchIds(policyIds);
        return policies.stream()
                .map(policy -> BeanUtil.copyProperties(policy, PolicyDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AgentDTO> getPolicyAgents(String policyId) {
        // 验证策略存在
        PolicyDO policy = policyMapper.selectById(policyId);
        if (ObjectUtil.isNull(policy)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }

        // 获取绑定的Agent ID列表
        List<String> agentIds = bindingMapper.selectAgentIdsByPolicyId(policyId);
        if (CollUtil.isEmpty(agentIds)) {
            return new ArrayList<>();
        }

        // 查询Agent详情
        List<AgentDO> agents = agentMapper.selectBatchIds(agentIds);
        return agents.stream()
                .map(agent -> BeanUtil.copyProperties(agent, AgentDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AgentPolicyBindingDTO> getBindingsByAgentId(String agentId) {
        // 验证Agent存在
        AgentDO agent = agentMapper.selectById(agentId);
        if (ObjectUtil.isNull(agent)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        LambdaQueryWrapper<AgentPolicyBindingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentPolicyBindingDO::getAgentId, agentId);
        List<AgentPolicyBindingDO> bindings = bindingMapper.selectList(wrapper);

        if (CollUtil.isEmpty(bindings)) {
            return new ArrayList<>();
        }

        // 获取所有策略ID
        List<String> policyIds = bindings.stream()
                .map(AgentPolicyBindingDO::getPolicyId)
                .collect(Collectors.toList());

        // 批量查询策略名称
        List<PolicyDO> policies = policyMapper.selectBatchIds(policyIds);
        java.util.Map<String, String> policyNameMap = policies.stream()
                .collect(Collectors.toMap(PolicyDO::getId, PolicyDO::getName));

        // 转换为DTO
        return bindings.stream()
                .map(binding -> toDTO(binding, agent.getName(), policyNameMap.get(binding.getPolicyId())))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBindingExists(String agentId, String policyId) {
        LambdaQueryWrapper<AgentPolicyBindingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentPolicyBindingDO::getAgentId, agentId)
               .eq(AgentPolicyBindingDO::getPolicyId, policyId);
        return bindingMapper.selectCount(wrapper) > 0;
    }

    private AgentPolicyBindingDTO toDTO(AgentPolicyBindingDO binding, String agentName, String policyName) {
        AgentPolicyBindingDTO dto = new AgentPolicyBindingDTO();
        dto.setId(binding.getId());
        dto.setAgentId(binding.getAgentId());
        dto.setPolicyId(binding.getPolicyId());
        dto.setAgentName(agentName);
        dto.setPolicyName(policyName);
        dto.setCreatedAt(binding.getCreatedAt());
        return dto;
    }
}
