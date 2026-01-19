package com.agentguard.budget.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import com.agentguard.budget.dto.BudgetCreateDTO;
import com.agentguard.budget.dto.BudgetDTO;
import com.agentguard.budget.dto.BudgetUpdateDTO;
import com.agentguard.budget.dto.BudgetWithUsageDTO;
import com.agentguard.budget.entity.BudgetDO;
import com.agentguard.budget.mapper.BudgetMapper;
import com.agentguard.budget.service.BudgetService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.stats.mapper.CostRecordMapper;
import com.agentguard.stats.dto.StatsOverviewDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 预算服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetMapper budgetMapper;
    private final CostRecordMapper costRecordMapper;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    @Transactional
    public BudgetDTO create(BudgetCreateDTO dto) {
        // 检查月份是否已存在
        if (budgetMapper.countByMonth(dto.getMonth()) > 0) {
            throw new BusinessException(ErrorCode.BUDGET_MONTH_DUPLICATE);
        }

        BudgetDO budgetDO = BeanUtil.copyProperties(dto, BudgetDO.class);
        budgetMapper.insert(budgetDO);

        return toDTO(budgetDO);
    }

    @Override
    public BudgetDTO getById(String id) {
        BudgetDO budgetDO = budgetMapper.selectById(id);
        if (ObjectUtil.isNull(budgetDO)) {
            throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
        }
        return toDTO(budgetDO);
    }

    @Override
    public IPage<BudgetDTO> page(Page<BudgetDTO> page) {
        Page<BudgetDO> doPage = new Page<>(page.getCurrent(), page.getSize());
        
        LambdaQueryWrapper<BudgetDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BudgetDO::getMonth);
        
        IPage<BudgetDO> resultPage = budgetMapper.selectPage(doPage, wrapper);
        
        return resultPage.convert(this::toDTO);
    }

    @Override
    @Transactional
    public BudgetDTO update(String id, BudgetUpdateDTO dto) {
        BudgetDO budgetDO = budgetMapper.selectById(id);
        if (ObjectUtil.isNull(budgetDO)) {
            throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
        }

        // 使用 Hutool 忽略空值拷贝
        BeanUtil.copyProperties(dto, budgetDO, CopyOptions.create().ignoreNullValue());
        budgetMapper.updateById(budgetDO);

        return toDTO(budgetDO);
    }

    @Override
    public BudgetWithUsageDTO getCurrentBudget() {
        // 获取当前月份
        String currentMonth = YearMonth.now().format(MONTH_FORMATTER);
        
        // 查询当月预算
        BudgetDO budgetDO = budgetMapper.selectByMonth(currentMonth);
        
        // 计算当月成本
        LocalDate startDate = YearMonth.now().atDay(1);
        LocalDate endDate = LocalDate.now();
        StatsOverviewDTO overview = costRecordMapper.selectOverview(startDate, endDate);
        
        BigDecimal usedAmount = BigDecimal.ZERO;
        if (ObjectUtil.isNotNull(overview) && ObjectUtil.isNotNull(overview.getTotalCost())) {
            usedAmount = overview.getTotalCost();
        }

        BudgetWithUsageDTO result = new BudgetWithUsageDTO();
        
        if (ObjectUtil.isNull(budgetDO)) {
            // 没有设置预算，返回默认值
            result.setMonth(currentMonth);
            result.setLimitAmount(BigDecimal.ZERO);
            result.setAlertThreshold(new BigDecimal("0.8"));
            result.setUsedAmount(usedAmount);
            result.setUsagePercentage(BigDecimal.ZERO);
            result.setRemainingAmount(BigDecimal.ZERO);
            result.setAlertTriggered(false);
            result.setOverBudget(false);
        } else {
            result.setId(budgetDO.getId());
            result.setMonth(budgetDO.getMonth());
            result.setLimitAmount(budgetDO.getLimitAmount());
            result.setAlertThreshold(budgetDO.getAlertThreshold());
            result.setUsedAmount(usedAmount);
            result.setCreatedAt(budgetDO.getCreatedAt());
            result.setUpdatedAt(budgetDO.getUpdatedAt());
            
            // 计算使用百分比
            if (budgetDO.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal usagePercentage = usedAmount.divide(budgetDO.getLimitAmount(), 4, RoundingMode.HALF_UP);
                result.setUsagePercentage(usagePercentage);
                result.setRemainingAmount(budgetDO.getLimitAmount().subtract(usedAmount).max(BigDecimal.ZERO));
                result.setAlertTriggered(usagePercentage.compareTo(budgetDO.getAlertThreshold()) >= 0);
                result.setOverBudget(usedAmount.compareTo(budgetDO.getLimitAmount()) > 0);
            } else {
                result.setUsagePercentage(BigDecimal.ZERO);
                result.setRemainingAmount(BigDecimal.ZERO);
                result.setAlertTriggered(false);
                result.setOverBudget(false);
            }
        }
        
        return result;
    }

    @Override
    public void checkAndAlert() {
        BudgetWithUsageDTO currentBudget = getCurrentBudget();
        
        if (ObjectUtil.isNull(currentBudget.getId())) {
            // 没有设置预算，不需要告警
            return;
        }
        
        if (currentBudget.getAlertTriggered()) {
            // 触发告警
            log.warn("预算告警：{}月预算使用已达{}%，已使用{}，预算上限{}",
                    currentBudget.getMonth(),
                    currentBudget.getUsagePercentage().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                    currentBudget.getUsedAmount(),
                    currentBudget.getLimitAmount());
            
            // TODO: 后续可以集成告警通知服务（邮件、短信、钉钉等）
        }
        
        if (currentBudget.getOverBudget()) {
            // 超预算告警
            log.error("预算超支告警：{}月已超出预算！已使用{}，预算上限{}",
                    currentBudget.getMonth(),
                    currentBudget.getUsedAmount(),
                    currentBudget.getLimitAmount());
            
            // TODO: 后续可以集成告警通知服务（邮件、短信、钉钉等）
        }
    }

    /**
     * 将实体转换为DTO
     *
     * @param budgetDO 预算实体
     * @return 预算DTO
     */
    private BudgetDTO toDTO(BudgetDO budgetDO) {
        return BeanUtil.copyProperties(budgetDO, BudgetDTO.class);
    }
}
