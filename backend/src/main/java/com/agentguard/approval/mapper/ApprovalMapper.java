package com.agentguard.approval.mapper;

import com.agentguard.approval.dto.ApprovalDTO;
import com.agentguard.approval.entity.ApprovalRequestDO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 审批请求数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface ApprovalMapper extends BaseMapper<ApprovalRequestDO> {

    /**
     * 分页查询审批请求
     *
     * @param page 分页参数
     * @param status 审批状态
     * @param agentId Agent ID
     * @param approvalId 审批ID（模糊查询）
     * @return 分页结果
     */
    IPage<ApprovalDTO> selectPageWithNames(Page<?> page,
                                           @Param("status") ApprovalStatus status,
                                           @Param("agentId") String agentId,
                                           @Param("approvalId") String approvalId);

    /**
     * 根据ID查询审批请求（带关联信息）
     *
     * @param id 审批请求ID
     * @return 审批请求DTO
     */
    ApprovalDTO selectByIdWithNames(@Param("id") String id);
}
