package com.agentguard.agent.mapper;

import com.agentguard.agent.entity.AgentDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface AgentMapper extends BaseMapper<AgentDO> {
}
