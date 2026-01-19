package com.agentguard.budget.service;

import com.agentguard.budget.dto.BudgetCreateDTO;
import com.agentguard.budget.dto.BudgetDTO;
import com.agentguard.budget.dto.BudgetUpdateDTO;
import com.agentguard.budget.dto.BudgetWithUsageDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 预算服务接口
 *
 * @author zhuhx
 */
public interface BudgetService {

    /**
     * 创建预算
     *
     * @param dto 创建预算请求
     * @return 预算信息
     */
    BudgetDTO create(BudgetCreateDTO dto);

    /**
     * 根据ID获取预算
     *
     * @param id 预算ID
     * @return 预算信息
     */
    BudgetDTO getById(String id);

    /**
     * 分页查询预算列表
     *
     * @param page 分页参数
     * @return 预算分页列表
     */
    IPage<BudgetDTO> page(Page<BudgetDTO> page);

    /**
     * 更新预算
     *
     * @param id  预算ID
     * @param dto 更新预算请求
     * @return 更新后的预算信息
     */
    BudgetDTO update(String id, BudgetUpdateDTO dto);

    /**
     * 获取当月预算及使用情况
     *
     * @return 当月预算及使用情况
     */
    BudgetWithUsageDTO getCurrentBudget();

    /**
     * 检查并触发预算告警
     */
    void checkAndAlert();
}
