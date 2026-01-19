package com.agentguard.user.controller;

import com.agentguard.common.response.Result;
import com.agentguard.user.dto.LoginDTO;
import com.agentguard.user.dto.LoginResponseDTO;
import com.agentguard.user.dto.RegisterDTO;
import com.agentguard.user.dto.UserDTO;
import com.agentguard.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author zhuhx
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、获取当前用户信息")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public Result<UserDTO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并获取Token")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    public Result<UserDTO> getCurrentUser() {
        return Result.success(authService.getCurrentUser());
    }
}
