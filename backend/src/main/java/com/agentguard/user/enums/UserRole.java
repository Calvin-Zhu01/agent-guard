package com.agentguard.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum UserRole {

    /** 管理员 */
    ADMIN("ADMIN", "管理员"),

    /** 普通用户 */
    USER("USER", "普通用户");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
