package com.agentguard.agent.service;

import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.dto.AgentPolicyBindingDTO;
import com.agentguard.policy.dto.PolicyDTO;

import java.util.List;

/**
 * Agent策略绑定服务接口
 *
 * @author zhuhx
 */
public interface AgentPolicyBindingService {

    /**
     * 绑定策略到Agent
     *
     * @param agentId Agent ID
     * @param policyId 策略ID
     * @return 绑定信息
     */
    AgentPolicyBindingDTO bindPolicy(String agentId, String policyId);

    /**
     * 解绑策略
     *
     * @param agentId Agent ID
     * @param policyId 策略ID
     */
    void unbindPolicy(String agentId, String policyId);

    /**
     * 获取Agent绑定的策略列表
     *
     * @param agentId Agent ID
     * @return 策略列表
     */
    List<PolicyDTO> getAgentPolicies(String agentId);

    /**
     * 获取策略绑定的Agent列表
     *
     * @param policyId 策略ID
     * @return Agent列表
     */
    List<AgentDTO> getPolicyAgents(String policyId);

    /**
     * 获取Agent的所有绑定记录
     *
     * @param agentId Agent ID
     * @return 绑定记录列表
     */
    List<AgentPolicyBindingDTO> getBindingsByAgentId(String agentId);

    /**
     * 检查绑定是否存在
     *
     * @param agentId Agent ID
     * @param policyId 策略ID
     * @return 是否存在
     */
    boolean isBindingExists(String agentId, String policyId);
}
