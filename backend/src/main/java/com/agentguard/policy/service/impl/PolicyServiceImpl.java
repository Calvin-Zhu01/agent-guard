package com.agentguard.policy.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.policy.dto.PolicyCreateDTO;
import com.agentguard.policy.dto.PolicyDTO;
import com.agentguard.policy.dto.PolicyUpdateDTO;
import com.agentguard.policy.engine.PolicyEngine;
import com.agentguard.policy.entity.PolicyDO;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import com.agentguard.policy.mapper.PolicyMapper;
import com.agentguard.policy.service.PolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 策略服务实现类
 *
 * @author zhuhx
 */
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyMapper policyMapper;
    private final PolicyEngine policyEngine;

    @Override
    @Transactional
    public PolicyDTO create(PolicyCreateDTO dto) {
        // 检查策略名称是否重复
        checkNameDuplicate(dto.getName(), null);

        // 验证条件JSON格式
        validateConditionsJson(dto.getConditions());

        PolicyDO policyDO = BeanUtil.copyProperties(dto, PolicyDO.class);
        policyDO.setEnabled(true);
        policyMapper.insert(policyDO);
        
        // 刷新策略缓存
        policyEngine.refreshPolicies();
        
        return toDTO(policyDO);
    }

    @Override
    public PolicyDTO getById(String id) {
        PolicyDO policyDO = policyMapper.selectById(id);
        if (ObjectUtil.isNull(policyDO)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }
        return toDTO(policyDO);
    }

    @Override
    public IPage<PolicyDTO> page(Page<PolicyDTO> page, String keyword, PolicyType type, PolicyScope scope, String sortBy) {
        LambdaQueryWrapper<PolicyDO> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(PolicyDO::getName, keyword)
                    .or().like(PolicyDO::getDescription, keyword));
        }

        if (ObjectUtil.isNotNull(type)) {
            wrapper.eq(PolicyDO::getType, type);
        }

        if (ObjectUtil.isNotNull(scope)) {
            wrapper.eq(PolicyDO::getScope, scope);
        }

        // 根据 sortBy 参数排序
        switch (StrUtil.blankToDefault(sortBy, "priority_desc")) {
            case "priority_asc" -> wrapper.orderByAsc(PolicyDO::getPriority).orderByDesc(PolicyDO::getUpdatedAt);
            case "updated_desc" -> wrapper.orderByDesc(PolicyDO::getUpdatedAt);
            case "updated_asc" -> wrapper.orderByAsc(PolicyDO::getUpdatedAt);
            default -> wrapper.orderByDesc(PolicyDO::getPriority).orderByDesc(PolicyDO::getUpdatedAt);
        }

        Page<PolicyDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<PolicyDO> result = policyMapper.selectPage(entityPage, wrapper);

        return result.convert(this::toDTO);
    }

    @Override
    @Transactional
    public PolicyDTO update(String id, PolicyUpdateDTO dto) {
        PolicyDO policyDO = policyMapper.selectById(id);
        if (ObjectUtil.isNull(policyDO)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }

        // 检查策略名称是否重复（排除自身）
        if (StrUtil.isNotBlank(dto.getName())) {
            checkNameDuplicate(dto.getName(), id);
        }

        // 验证条件JSON格式
        if (StrUtil.isNotBlank(dto.getConditions())) {
            validateConditionsJson(dto.getConditions());
        }

        // 使用 Hutool 忽略空值拷贝
        BeanUtil.copyProperties(dto, policyDO, CopyOptions.create().ignoreNullValue());
        
        policyDO.setUpdatedAt(LocalDateTime.now());

        policyMapper.updateById(policyDO);
        
        // 刷新策略缓存
        policyEngine.refreshPolicies();
        
        return toDTO(policyDO);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (policyMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }
        
        // 刷新策略缓存
        policyEngine.refreshPolicies();
    }

    @Override
    @Transactional
    public void enable(String id) {
        // 检查策略是否存在
        PolicyDO policyDO = policyMapper.selectById(id);
        if (ObjectUtil.isNull(policyDO)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }

        LambdaUpdateWrapper<PolicyDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PolicyDO::getId, id)
                     .set(PolicyDO::getEnabled, true);
        policyMapper.update(null, updateWrapper);

        // 刷新策略缓存
        policyEngine.refreshPolicies();
    }

    @Override
    @Transactional
    public void disable(String id) {
        // 检查策略是否存在
        PolicyDO policyDO = policyMapper.selectById(id);
        if (ObjectUtil.isNull(policyDO)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }

        LambdaUpdateWrapper<PolicyDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PolicyDO::getId, id)
                     .set(PolicyDO::getEnabled, false);
        policyMapper.update(null, updateWrapper);

        // 刷新策略缓存
        policyEngine.refreshPolicies();
    }

    @Override
    public List<PolicyDTO> getEnabledPolicies() {
        LambdaQueryWrapper<PolicyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyDO::getEnabled, true)
               .orderByDesc(PolicyDO::getPriority);

        List<PolicyDO> policies = policyMapper.selectList(wrapper);
        return policies.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查策略名称是否重复
     *
     * @param name 策略名称
     * @param excludeId 排除的策略ID（更新时使用）
     */
    private void checkNameDuplicate(String name, String excludeId) {
        LambdaQueryWrapper<PolicyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyDO::getName, name);
        if (StrUtil.isNotBlank(excludeId)) {
            wrapper.ne(PolicyDO::getId, excludeId);
        }

        if (policyMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.POLICY_NAME_DUPLICATE);
        }
    }

    /**
     * 验证条件JSON格式
     *
     * @param conditions 条件JSON字符串
     */
    private void validateConditionsJson(String conditions) {
        if (StrUtil.isNotBlank(conditions)) {
            if (!JSONUtil.isTypeJSON(conditions)) {
                throw new BusinessException(ErrorCode.POLICY_CONDITION_INVALID);
            }
        }
    }

    /**
     * 转换为DTO
     *
     * @param policyDO 实体对象
     * @return DTO对象
     */
    private PolicyDTO toDTO(PolicyDO policyDO) {
        return BeanUtil.copyProperties(policyDO, PolicyDTO.class);
    }
}
