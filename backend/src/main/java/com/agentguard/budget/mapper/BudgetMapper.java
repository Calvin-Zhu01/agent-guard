package com.agentguard.budget.mapper;

import com.agentguard.budget.entity.BudgetDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 预算数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface BudgetMapper extends BaseMapper<BudgetDO> {

    /**
     * 根据月份查询预算
     *
     * @param month 月份（格式：YYYY-MM）
     * @return 预算记录
     */
    @Select("SELECT * FROM budget WHERE month = #{month}")
    BudgetDO selectByMonth(@Param("month") String month);

    /**
     * 检查月份是否已存在预算
     *
     * @param month 月份（格式：YYYY-MM）
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(1) FROM budget WHERE month = #{month}")
    int countByMonth(@Param("month") String month);

    /**
     * 检查月份是否已存在预算（排除指定ID）
     *
     * @param month 月份（格式：YYYY-MM）
     * @param excludeId 排除的ID
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(1) FROM budget WHERE month = #{month} AND id != #{excludeId}")
    int countByMonthExcludeId(@Param("month") String month, @Param("excludeId") String excludeId);
}
