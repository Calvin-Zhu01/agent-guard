package com.agentguard.alert.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.agentguard.alert.dto.AlertHistoryDTO;
import com.agentguard.alert.entity.AlertHistoryDO;
import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.mapper.AlertHistoryMapper;
import com.agentguard.alert.service.AlertHistoryService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警历史服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertHistoryServiceImpl implements AlertHistoryService {

    private final AlertHistoryMapper alertHistoryMapper;

    @Override
    public AlertHistoryDTO getById(String id) {
        AlertHistoryDO alertHistoryDO = alertHistoryMapper.selectById(id);
        if (ObjectUtil.isNull(alertHistoryDO)) {
            throw new BusinessException(ErrorCode.ALERT_HISTORY_NOT_FOUND);
        }
        return toDTO(alertHistoryDO);
    }

    @Override
    public IPage<AlertHistoryDTO> page(Page<AlertHistoryDTO> page, AlertType type, AlertStatus status,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AlertHistoryDO> wrapper = buildQueryWrapper(type, status, startTime, endTime);
        wrapper.orderByDesc(AlertHistoryDO::getCreatedAt);

        Page<AlertHistoryDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<AlertHistoryDO> result = alertHistoryMapper.selectPage(entityPage, wrapper);

        return result.convert(this::toDTO);
    }

    @Override
    public long count(AlertType type, AlertStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AlertHistoryDO> wrapper = buildQueryWrapper(type, status, startTime, endTime);
        return alertHistoryMapper.selectCount(wrapper);
    }

    @Override
    public List<AlertHistoryDTO> export(AlertType type, AlertStatus status, 
                                         LocalDateTime startTime, LocalDateTime endTime) {
        log.info("导出告警历史: type={}, status={}, startTime={}, endTime={}", type, status, startTime, endTime);
        
        LambdaQueryWrapper<AlertHistoryDO> wrapper = buildQueryWrapper(type, status, startTime, endTime);
        wrapper.orderByDesc(AlertHistoryDO::getCreatedAt);
        
        List<AlertHistoryDO> historyList = alertHistoryMapper.selectList(wrapper);
        
        log.info("导出告警历史记录数: {}", historyList.size());
        
        return historyList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<AlertHistoryDTO> pageByRuleId(String ruleId, Page<AlertHistoryDTO> page) {
        LambdaQueryWrapper<AlertHistoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertHistoryDO::getRuleId, ruleId)
               .orderByDesc(AlertHistoryDO::getCreatedAt);

        Page<AlertHistoryDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<AlertHistoryDO> result = alertHistoryMapper.selectPage(entityPage, wrapper);

        return result.convert(this::toDTO);
    }

    @Override
    public List<AlertHistoryDTO> getRecent(int limit) {
        LambdaQueryWrapper<AlertHistoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AlertHistoryDO::getCreatedAt)
               .last("LIMIT " + limit);

        List<AlertHistoryDO> historyList = alertHistoryMapper.selectList(wrapper);
        
        return historyList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertHistoryDTO> countByType(LocalDateTime startTime, LocalDateTime endTime) {
        // 返回各类型的告警统计，这里简化处理，返回每种类型的最新一条记录
        // 实际统计可以通过自定义SQL实现
        LambdaQueryWrapper<AlertHistoryDO> wrapper = new LambdaQueryWrapper<>();
        
        if (ObjectUtil.isNotNull(startTime)) {
            wrapper.ge(AlertHistoryDO::getCreatedAt, startTime);
        }
        
        if (ObjectUtil.isNotNull(endTime)) {
            wrapper.le(AlertHistoryDO::getCreatedAt, endTime);
        }
        
        wrapper.orderByDesc(AlertHistoryDO::getCreatedAt);
        
        List<AlertHistoryDO> historyList = alertHistoryMapper.selectList(wrapper);
        
        return historyList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 构建查询条件
     *
     * @param type      告警类型
     * @param status    发送状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 查询条件
     */
    private LambdaQueryWrapper<AlertHistoryDO> buildQueryWrapper(AlertType type, AlertStatus status,
                                                                  LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AlertHistoryDO> wrapper = new LambdaQueryWrapper<>();

        if (ObjectUtil.isNotNull(type)) {
            wrapper.eq(AlertHistoryDO::getType, type);
        }

        if (ObjectUtil.isNotNull(status)) {
            wrapper.eq(AlertHistoryDO::getStatus, status);
        }

        if (ObjectUtil.isNotNull(startTime)) {
            wrapper.ge(AlertHistoryDO::getCreatedAt, startTime);
        }

        if (ObjectUtil.isNotNull(endTime)) {
            wrapper.le(AlertHistoryDO::getCreatedAt, endTime);
        }

        return wrapper;
    }

    /**
     * 转换为DTO
     *
     * @param alertHistoryDO 实体对象
     * @return DTO对象
     */
    private AlertHistoryDTO toDTO(AlertHistoryDO alertHistoryDO) {
        return BeanUtil.copyProperties(alertHistoryDO, AlertHistoryDTO.class);
    }
}
