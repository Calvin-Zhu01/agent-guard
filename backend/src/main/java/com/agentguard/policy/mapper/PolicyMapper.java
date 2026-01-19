package com.agentguard.policy.mapper;

import com.agentguard.policy.entity.PolicyDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 策略数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface PolicyMapper extends BaseMapper<PolicyDO> {
}
