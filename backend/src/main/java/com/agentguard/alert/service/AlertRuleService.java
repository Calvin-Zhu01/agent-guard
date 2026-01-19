package com.agentguard.alert.service;

import com.agentguard.alert.dto.AlertRuleCreateDTO;
import com.agentguard.alert.dto.AlertRuleDTO;
import com.agentguard.alert.dto.AlertRuleUpdateDTO;
import com.agentguard.alert.enums.AlertType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 告警规则服务接口
 *
 * @author zhuhx
 */
public interface AlertRuleService {

    /**
     * 创建告警规则
     *
     * @param dto 创建请求
     * @return 告警规则信息
     */
    AlertRuleDTO create(AlertRuleCreateDTO dto);

    /**
     * 根据ID获取告警规则
     *
     * @param id 规则ID
     * @return 告警规则信息
     */
    AlertRuleDTO getById(String id);

    /**
     * 分页查询告警规则
     *
     * @param page    分页参数
     * @param keyword 关键词（名称模糊搜索）
     * @param type    告警类型
     * @param enabled 是否启用
     * @return 分页结果
     */
    IPage<AlertRuleDTO> page(Page<AlertRuleDTO> page, String keyword, AlertType type, Boolean enabled);

    /**
     * 更新告警规则
     *
     * @param id  规则ID
     * @param dto 更新请求
     * @return 更新后的告警规则信息
     */
    AlertRuleDTO update(String id, AlertRuleUpdateDTO dto);

    /**
     * 删除告警规则
     *
     * @param id 规则ID
     */
    void delete(String id);

    /**
     * 启用告警规则
     *
     * @param id 规则ID
     */
    void enable(String id);

    /**
     * 停用告警规则
     *
     * @param id 规则ID
     */
    void disable(String id);

    /**
     * 获取所有启用的告警规则
     *
     * @return 告警规则列表
     */
    List<AlertRuleDTO> getEnabledRules();

    /**
     * 根据类型获取启用的告警规则
     *
     * @param type 告警类型
     * @return 告警规则列表
     */
    List<AlertRuleDTO> getEnabledRulesByType(AlertType type);
}
