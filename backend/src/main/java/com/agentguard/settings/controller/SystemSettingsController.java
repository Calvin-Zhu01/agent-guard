package com.agentguard.settings.controller;

import com.agentguard.common.response.Result;
import com.agentguard.settings.dto.AlertSettingsDTO;
import com.agentguard.settings.dto.EmailSettingsDTO;
import com.agentguard.settings.dto.WebhookSettingsDTO;
import com.agentguard.settings.service.SystemSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统设置控制器
 *
 * @author zhuhx
 */
@Tag(name = "系统设置", description = "系统设置管理接口")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    // ==================== 邮件通知配置 ====================

    @Operation(summary = "获取邮件通知配置")
    @GetMapping("/email")
    public Result<EmailSettingsDTO> getEmailSettings() {
        return Result.success(systemSettingsService.getEmailSettings());
    }

    @Operation(summary = "更新邮件通知配置")
    @PutMapping("/email")
    public Result<Void> updateEmailSettings(@Valid @RequestBody EmailSettingsDTO dto) {
        systemSettingsService.updateEmailSettings(dto);
        return Result.success();
    }

    @Operation(summary = "测试邮件配置")
    @PostMapping("/email/test")
    public Result<Boolean> testEmailSettings(@Valid @RequestBody EmailSettingsDTO dto) {
        boolean success = systemSettingsService.testEmailSettings(dto);
        return Result.success(success);
    }

    // ==================== Webhook通知配置 ====================

    @Operation(summary = "获取Webhook通知配置")
    @GetMapping("/webhook")
    public Result<WebhookSettingsDTO> getWebhookSettings() {
        return Result.success(systemSettingsService.getWebhookSettings());
    }

    @Operation(summary = "更新Webhook通知配置")
    @PutMapping("/webhook")
    public Result<Void> updateWebhookSettings(@Valid @RequestBody WebhookSettingsDTO dto) {
        systemSettingsService.updateWebhookSettings(dto);
        return Result.success();
    }

    // ==================== 告警配置 ====================

    @Operation(summary = "获取告警配置")
    @GetMapping("/alert")
    public Result<AlertSettingsDTO> getAlertSettings() {
        return Result.success(systemSettingsService.getAlertSettings());
    }

    @Operation(summary = "更新告警配置")
    @PutMapping("/alert")
    public Result<Void> updateAlertSettings(@Valid @RequestBody AlertSettingsDTO dto) {
        systemSettingsService.updateAlertSettings(dto);
        return Result.success();
    }
}
