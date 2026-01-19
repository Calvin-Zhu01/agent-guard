package com.agentguard.common.exception;

import cn.hutool.core.collection.CollUtil;
import com.agentguard.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 全局异常处理器
 *
 * @author zhuhx
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = extractFirstFieldError(e.getBindingResult().getFieldErrors(), "参数校验失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = extractFirstFieldError(e.getBindingResult().getFieldErrors(), "参数绑定失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统内部错误");
    }

    /**
     * 提取第一个字段错误信息
     *
     * @param fieldErrors 字段错误列表
     * @param defaultMessage 默认消息
     * @return 错误消息
     */
    private String extractFirstFieldError(List<FieldError> fieldErrors, String defaultMessage) {
        if (CollUtil.isEmpty(fieldErrors)) {
            return defaultMessage;
        }
        FieldError firstError = CollUtil.getFirst(fieldErrors);
        return firstError.getField() + ": " + firstError.getDefaultMessage();
    }
}
