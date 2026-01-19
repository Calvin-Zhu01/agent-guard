package com.agentguard.user.mapper;

import com.agentguard.user.entity.UserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * @author zhuhx
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
