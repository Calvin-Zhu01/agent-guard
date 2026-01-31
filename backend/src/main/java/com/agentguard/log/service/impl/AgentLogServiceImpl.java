package com.agentguard.log.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.log.dto.AgentLogCreateDTO;
import com.agentguard.log.dto.AgentLogDTO;
import com.agentguard.log.entity.AgentLogDO;
import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.agentguard.log.mapper.AgentLogMapper;
import com.agentguard.log.service.AgentLogService;
import com.agentguard.stats.service.StatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent日志服务实现类
 *
 * @author zhuhx
 */
@Service
@RequiredArgsConstructor
public class AgentLogServiceImpl implements AgentLogService {

    private final AgentLogMapper agentLogMapper;
    private final AgentMapper agentMapper;
    private final StatsService statsService;

    @Override
    @Transactional
    public AgentLogDTO create(AgentLogCreateDTO dto) {
        AgentLogDO logDO = BeanUtil.copyProperties(dto, AgentLogDO.class);
        agentLogMapper.insert(logDO);
        
        // 同步更新成本记录
        updateCostRecord(dto);
        
        return BeanUtil.copyProperties(logDO, AgentLogDTO.class);
    }
    
    /**
     * 更新成本记录
     *
     * @param dto 日志创建DTO
     */
    private void updateCostRecord(AgentLogCreateDTO dto) {
        boolean isApiCall = RequestType.API_CALL.equals(dto.getRequestType());
        boolean isLlmCall = RequestType.LLM_CALL.equals(dto.getRequestType());
        
        BigDecimal llmCost = BigDecimal.ZERO;
        BigDecimal apiCost = BigDecimal.ZERO;
        
        if (ObjectUtil.isNotNull(dto.getCost())) {
            if (isLlmCall) {
                llmCost = dto.getCost();
            } else if (isApiCall) {
                apiCost = dto.getCost();
            }
        }
        
        statsService.updateCostRecord(
                dto.getAgentId(),
                dto.getModel(),
                dto.getTokenInput(),
                dto.getTokenOutput(),
                llmCost,
                apiCost,
                isApiCall || isLlmCall
        );
    }

    @Override
    public AgentLogDTO getById(String id) {
        AgentLogDO logDO = agentLogMapper.selectById(id);
        if (ObjectUtil.isNull(logDO)) {
            throw new BusinessException(ErrorCode.LOG_NOT_FOUND);
        }
        
        // 查询Agent名称
        AgentDO agentDO = agentMapper.selectById(logDO.getAgentId());
        String agentName = ObjectUtil.isNotNull(agentDO) ? agentDO.getName() : null;
        
        AgentLogDTO dto = BeanUtil.copyProperties(logDO, AgentLogDTO.class);
        dto.setAgentName(agentName);
        return dto;
    }

    @Override
    public IPage<AgentLogDTO> page(Page<AgentLogDTO> page, String agentId, ResponseStatus responseStatus, RequestType requestType) {
        LambdaQueryWrapper<AgentLogDO> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(agentId)) {
            wrapper.eq(AgentLogDO::getAgentId, agentId);
        }
        if (responseStatus != null) {
            wrapper.eq(AgentLogDO::getResponseStatus, responseStatus);
        }
        if (requestType != null) {
            wrapper.eq(AgentLogDO::getRequestType, requestType);
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(AgentLogDO::getCreatedAt);

        Page<AgentLogDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<AgentLogDO> result = agentLogMapper.selectPage(entityPage, wrapper);

        // 批量查询Agent名称
        Set<String> agentIds = result.getRecords().stream()
                .map(AgentLogDO::getAgentId)
                .collect(Collectors.toSet());
        
        Map<String, String> agentNameMap = Map.of();
        if (CollUtil.isNotEmpty(agentIds)) {
            LambdaQueryWrapper<AgentDO> agentWrapper = new LambdaQueryWrapper<>();
            agentWrapper.in(AgentDO::getId, agentIds);
            agentNameMap = agentMapper.selectList(agentWrapper).stream()
                    .collect(Collectors.toMap(AgentDO::getId, AgentDO::getName));
        }

        Map<String, String> finalAgentNameMap = agentNameMap;
        return result.convert(logDO -> {
            AgentLogDTO dto = BeanUtil.copyProperties(logDO, AgentLogDTO.class);
            dto.setAgentName(finalAgentNameMap.get(logDO.getAgentId()));
            return dto;
        });
    }

    @Override
    @Transactional
    public void updateStatusByApprovalRequestId(String approvalRequestId, ResponseStatus newStatus) {
        if (StrUtil.isBlank(approvalRequestId)) {
            return;
        }

        LambdaUpdateWrapper<AgentLogDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AgentLogDO::getApprovalRequestId, approvalRequestId)
               .set(AgentLogDO::getResponseStatus, newStatus);

        agentLogMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void updateResponseBodyByApprovalRequestId(String approvalRequestId, String responseBody) {
        if (StrUtil.isBlank(approvalRequestId)) {
            return;
        }

        LambdaUpdateWrapper<AgentLogDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AgentLogDO::getApprovalRequestId, approvalRequestId)
               .set(AgentLogDO::getResponseBody, responseBody);

        agentLogMapper.update(null, wrapper);
    }
}
