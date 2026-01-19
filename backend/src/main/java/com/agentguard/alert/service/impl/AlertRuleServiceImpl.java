package com.agentguard.alert.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.alert.dto.AlertRuleCreateDTO;
import com.agentguard.alert.dto.AlertRuleDTO;
import com.agentguard.alert.dto.AlertRuleUpdateDTO;
import com.agentguard.alert.entity.AlertRuleDO;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.mapper.AlertRuleMapper;
import com.agentguard.alert.service.AlertRuleService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警规则服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;

    @Override
    @Transactional
    public AlertRuleDTO create(AlertRuleCreateDTO dto) {
        // 检查规则名称是否重复
        checkNameDuplicate(dto.getName(), null);

        AlertRuleDO alertRuleDO = BeanUtil.copyProperties(dto, AlertRuleDO.class);
        alertRuleDO.setEnabled(true);
        alertRuleMapper.insert(alertRuleDO);

        log.info("创建告警规则: id={}, name={}, type={}", alertRuleDO.getId(), alertRuleDO.getName(), alertRuleDO.getType());
        return toDTO(alertRuleDO);
    }

    @Override
    public AlertRuleDTO getById(String id) {
        AlertRuleDO alertRuleDO = alertRuleMapper.selectById(id);
        if (ObjectUtil.isNull(alertRuleDO)) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }
        return toDTO(alertRuleDO);
    }


    @Override
    public IPage<AlertRuleDTO> page(Page<AlertRuleDTO> page, String keyword, AlertType type, Boolean enabled) {
        LambdaQueryWrapper<AlertRuleDO> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(AlertRuleDO::getName, keyword);
        }

        if (ObjectUtil.isNotNull(type)) {
            wrapper.eq(AlertRuleDO::getType, type);
        }

        if (ObjectUtil.isNotNull(enabled)) {
            wrapper.eq(AlertRuleDO::getEnabled, enabled);
        }

        wrapper.orderByDesc(AlertRuleDO::getCreatedAt);

        Page<AlertRuleDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<AlertRuleDO> result = alertRuleMapper.selectPage(entityPage, wrapper);

        return result.convert(this::toDTO);
    }

    @Override
    @Transactional
    public AlertRuleDTO update(String id, AlertRuleUpdateDTO dto) {
        AlertRuleDO alertRuleDO = alertRuleMapper.selectById(id);
        if (ObjectUtil.isNull(alertRuleDO)) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }

        // 检查规则名称是否重复（排除自身）
        if (StrUtil.isNotBlank(dto.getName())) {
            checkNameDuplicate(dto.getName(), id);
        }

        // 使用 Hutool 忽略空值拷贝
        BeanUtil.copyProperties(dto, alertRuleDO, CopyOptions.create().ignoreNullValue());

        alertRuleMapper.updateById(alertRuleDO);
        log.info("更新告警规则: id={}, name={}", alertRuleDO.getId(), alertRuleDO.getName());
        return toDTO(alertRuleDO);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (alertRuleMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }
        log.info("删除告警规则: id={}", id);
    }

    @Override
    @Transactional
    public void enable(String id) {
        AlertRuleDO alertRuleDO = alertRuleMapper.selectById(id);
        if (ObjectUtil.isNull(alertRuleDO)) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }
        alertRuleDO.setEnabled(true);
        alertRuleMapper.updateById(alertRuleDO);
        log.info("启用告警规则: id={}, name={}", alertRuleDO.getId(), alertRuleDO.getName());
    }

    @Override
    @Transactional
    public void disable(String id) {
        AlertRuleDO alertRuleDO = alertRuleMapper.selectById(id);
        if (ObjectUtil.isNull(alertRuleDO)) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }
        alertRuleDO.setEnabled(false);
        alertRuleMapper.updateById(alertRuleDO);
        log.info("停用告警规则: id={}, name={}", alertRuleDO.getId(), alertRuleDO.getName());
    }

    @Override
    public List<AlertRuleDTO> getEnabledRules() {
        LambdaQueryWrapper<AlertRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRuleDO::getEnabled, true)
               .orderByDesc(AlertRuleDO::getCreatedAt);

        List<AlertRuleDO> rules = alertRuleMapper.selectList(wrapper);
        return rules.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertRuleDTO> getEnabledRulesByType(AlertType type) {
        LambdaQueryWrapper<AlertRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRuleDO::getEnabled, true)
               .eq(AlertRuleDO::getType, type)
               .orderByDesc(AlertRuleDO::getCreatedAt);

        List<AlertRuleDO> rules = alertRuleMapper.selectList(wrapper);
        return rules.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查规则名称是否重复
     *
     * @param name      规则名称
     * @param excludeId 排除的规则ID（更新时使用）
     */
    private void checkNameDuplicate(String name, String excludeId) {
        LambdaQueryWrapper<AlertRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRuleDO::getName, name);
        if (StrUtil.isNotBlank(excludeId)) {
            wrapper.ne(AlertRuleDO::getId, excludeId);
        }

        if (alertRuleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NAME_DUPLICATE);
        }
    }

    /**
     * 转换为DTO
     *
     * @param alertRuleDO 实体对象
     * @return DTO对象
     */
    private AlertRuleDTO toDTO(AlertRuleDO alertRuleDO) {
        return BeanUtil.copyProperties(alertRuleDO, AlertRuleDTO.class);
    }
}
