package com.agentguard.alert.mapper;

import com.agentguard.alert.entity.AlertRuleDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRuleDO> {
}
