package com.agentguard.agent.mapper;

import com.agentguard.agent.entity.AgentPolicyBindingDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * Agent策略绑定数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface AgentPolicyBindingMapper extends BaseMapper<AgentPolicyBindingDO> {

    /**
     * 根据Agent ID查询绑定的策略ID列表
     *
     * @param agentId Agent ID
     * @return 策略ID列表
     */
    @Select("SELECT policy_id FROM agent_policy_binding WHERE agent_id = #{agentId}")
    List<String> selectPolicyIdsByAgentId(@Param("agentId") String agentId);

    /**
     * 根据策略ID查询绑定的Agent ID列表
     *
     * @param policyId 策略ID
     * @return Agent ID列表
     */
    @Select("SELECT agent_id FROM agent_policy_binding WHERE policy_id = #{policyId}")
    List<String> selectAgentIdsByPolicyId(@Param("policyId") String policyId);

    /**
     * 批量查询Agent的策略绑定关系
     *
     * @param agentIds Agent ID集合
     * @return 绑定关系列表
     */
    List<AgentPolicyBindingDO> selectByAgentIds(@Param("agentIds") Collection<String> agentIds);
}
