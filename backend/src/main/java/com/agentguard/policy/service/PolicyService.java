package com.agentguard.policy.service;

import com.agentguard.policy.dto.PolicyCreateDTO;
import com.agentguard.policy.dto.PolicyDTO;
import com.agentguard.policy.dto.PolicyUpdateDTO;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 策略服务接口
 *
 * @author zhuhx
 */
public interface PolicyService {

    /**
     * 创建策略
     *
     * @param dto 创建请求
     * @return 策略信息
     */
    PolicyDTO create(PolicyCreateDTO dto);

    /**
     * 根据ID获取策略详情
     *
     * @param id 策略ID
     * @return 策略信息
     */
    PolicyDTO getById(String id);

    /**
     * 分页查询策略列表
     *
     * @param page 分页参数
     * @param keyword 关键词（名称/描述）
     * @param type 策略类型
     * @param scope 作用域
     * @param sortBy 排序方式：priority_desc/priority_asc/created_desc/created_asc
     * @return 分页结果
     */
    IPage<PolicyDTO> page(Page<PolicyDTO> page, String keyword, PolicyType type, PolicyScope scope, String sortBy);

    /**
     * 更新策略
     *
     * @param id 策略ID
     * @param dto 更新请求
     * @return 策略信息
     */
    PolicyDTO update(String id, PolicyUpdateDTO dto);

    /**
     * 删除策略（逻辑删除）
     *
     * @param id 策略ID
     */
    void delete(String id);

    /**
     * 启用策略
     *
     * @param id 策略ID
     */
    void enable(String id);

    /**
     * 停用策略
     *
     * @param id 策略ID
     */
    void disable(String id);

    /**
     * 获取所有已启用的策略
     *
     * @return 已启用策略列表
     */
    List<PolicyDTO> getEnabledPolicies();
}
