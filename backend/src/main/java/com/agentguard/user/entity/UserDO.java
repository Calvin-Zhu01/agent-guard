package com.agentguard.user.entity;

import com.agentguard.user.enums.UserRole;
import com.agentguard.user.enums.UserStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("user")
public class UserDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 邮箱 */
    private String email;

    /** 角色：ADMIN-管理员，USER-普通用户 */
    private UserRole role;

    /** 状态：0-禁用，1-启用 */
    private UserStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
