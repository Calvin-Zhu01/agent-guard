package com.agentguard.log.service;

import com.agentguard.log.dto.AgentLogCreateDTO;
import com.agentguard.log.dto.AgentLogDTO;
import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * Agent日志服务接口
 *
 * @author zhuhx
 */
public interface AgentLogService {

    /**
     * 创建日志
     *
     * @param dto 创建请求
     * @return 日志信息
     */
    AgentLogDTO create(AgentLogCreateDTO dto);

    /**
     * 根据ID获取日志
     *
     * @param id 日志ID
     * @return 日志信息
     */
    AgentLogDTO getById(String id);

    /**
     * 分页查询日志
     *
     * @param page 分页参数
     * @param agentId Agent ID过滤（可选）
     * @param responseStatus 响应状态过滤（可选）
     * @param requestType 请求类型过滤（可选）
     * @return 分页结果
     */
    IPage<AgentLogDTO> page(Page<AgentLogDTO> page, String agentId, ResponseStatus responseStatus, RequestType requestType);

    /**
     * 根据审批请求ID更新日志状态
     *
     * @param approvalRequestId 审批请求ID
     * @param newStatus 新的响应状态
     */
    void updateStatusByApprovalRequestId(String approvalRequestId, ResponseStatus newStatus);
}
