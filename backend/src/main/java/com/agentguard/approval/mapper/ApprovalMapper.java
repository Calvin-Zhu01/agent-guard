package com.agentguard.approval.mapper;

import com.agentguard.approval.entity.ApprovalRequestDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批请求数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface ApprovalMapper extends BaseMapper<ApprovalRequestDO> {
}
