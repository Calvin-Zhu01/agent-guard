package com.agentguard.log.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 请求类型枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum RequestType {

    /** API调用 */
    API_CALL("API_CALL", "API调用"),

    /** LLM调用 */
    LLM_CALL("LLM_CALL", "LLM调用"),

    /** 全部（策略适用于所有类型） */
    ALL("ALL", "全部");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
