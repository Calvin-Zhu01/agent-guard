package com.agentguard.settings.mapper;

import com.agentguard.settings.entity.SystemSettingsDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置 Mapper
 *
 * @author zhuhx
 */
@Mapper
public interface SystemSettingsMapper extends BaseMapper<SystemSettingsDO> {
}
