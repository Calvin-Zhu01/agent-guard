package com.agentguard.proxy.enums;

import cn.hutool.core.util.StrUtil;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpMethod;

/**
 * HTTP 方法枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum HttpMethodType {

    /** GET 方法 */
    GET("GET", "查询"),

    /** POST 方法 */
    POST("POST", "创建"),

    /** PUT 方法 */
    PUT("PUT", "更新"),

    /** DELETE 方法 */
    DELETE("DELETE", "删除"),

    /** PATCH 方法 */
    PATCH("PATCH", "部分更新");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    /**
     * 转换为 Spring HttpMethod
     */
    public HttpMethod toSpringHttpMethod() {
        return HttpMethod.valueOf(this.code);
    }

    /**
     * 从字符串解析
     */
    public static HttpMethodType fromString(String method) {
        if (StrUtil.isBlank(method)) {
            return POST;
        }

        for (HttpMethodType type : values()) {
            if (type.code.equalsIgnoreCase(method)) {
                return type;
            }
        }

        throw new BusinessException(ErrorCode.UNSUPPORTED_HTTP_METHOD);
    }
}
