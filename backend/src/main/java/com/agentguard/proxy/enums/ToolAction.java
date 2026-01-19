package com.agentguard.proxy.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Tool Schema 操作类型枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum ToolAction {

    /** 转账 */
    TRANSFER_FUNDS("transfer_funds", "转账", "POST"),

    /** 发送邮件 */
    SEND_EMAIL("send_email", "发送邮件", "POST"),

    /** 读取文档 */
    READ_DOCUMENT("read_document", "读取文档", "GET"),

    /** 写入数据库 */
    WRITE_DATABASE("write_database", "写入数据库", "POST"),

    /** 调用API */
    CALL_API("call_api", "调用API", "POST"),

    /** 调用LLM */
    INVOKE_LLM("invoke_llm", "调用LLM", "POST");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    /** 默认HTTP方法 */
    private final String defaultMethod;

    /**
     * 根据code获取枚举值
     *
     * @param code 操作类型code
     * @return 枚举值，如果不存在返回null
     */
    public static ToolAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ToolAction action : values()) {
            if (action.getCode().equalsIgnoreCase(code)) {
                return action;
            }
        }
        return null;
    }
}
