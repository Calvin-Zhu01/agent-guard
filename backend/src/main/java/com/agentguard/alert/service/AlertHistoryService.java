package com.agentguard.alert.service;

import com.agentguard.alert.dto.AlertHistoryDTO;
import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警历史服务接口
 *
 * @author zhuhx
 */
public interface AlertHistoryService {

    /**
     * 根据ID获取告警历史
     *
     * @param id 历史记录ID
     * @return 告警历史信息
     */
    AlertHistoryDTO getById(String id);

    /**
     * 分页查询告警历史
     *
     * @param page      分页参数
     * @param type      告警类型
     * @param status    发送状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页结果
     */
    IPage<AlertHistoryDTO> page(Page<AlertHistoryDTO> page, AlertType type, AlertStatus status,
                                 LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计告警数量
     *
     * @param type      告警类型
     * @param status    发送状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 告警数量
     */
    long count(AlertType type, AlertStatus status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 导出告警历史数据
     * 根据筛选条件查询所有符合条件的告警历史记录
     *
     * @param type      告警类型
     * @param status    发送状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 告警历史列表
     */
    List<AlertHistoryDTO> export(AlertType type, AlertStatus status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据规则ID查询告警历史
     *
     * @param ruleId 告警规则ID
     * @param page   分页参数
     * @return 分页结果
     */
    IPage<AlertHistoryDTO> pageByRuleId(String ruleId, Page<AlertHistoryDTO> page);

    /**
     * 获取最近的告警历史
     *
     * @param limit 限制数量
     * @return 告警历史列表
     */
    List<AlertHistoryDTO> getRecent(int limit);

    /**
     * 统计各类型告警数量
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 各类型告警数量统计
     */
    List<AlertHistoryDTO> countByType(LocalDateTime startTime, LocalDateTime endTime);
}
