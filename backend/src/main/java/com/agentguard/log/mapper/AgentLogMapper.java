package com.agentguard.log.mapper;

import com.agentguard.log.entity.AgentLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent日志数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface AgentLogMapper extends BaseMapper<AgentLogDO> {
}
