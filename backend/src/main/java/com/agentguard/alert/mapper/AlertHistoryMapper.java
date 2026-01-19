package com.agentguard.alert.mapper;

import com.agentguard.alert.entity.AlertHistoryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警历史数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface AlertHistoryMapper extends BaseMapper<AlertHistoryDO> {
}
