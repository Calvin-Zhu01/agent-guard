package com.agentguard.user.service;

import com.agentguard.user.dto.LoginDTO;
import com.agentguard.user.dto.LoginResponseDTO;
import com.agentguard.user.dto.RegisterDTO;
import com.agentguard.user.dto.UserDTO;

/**
 * 认证服务接口
 *
 * @author zhuhx
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param dto 注册信息
     * @return 用户信息
     */
    UserDTO register(RegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return 登录响应（包含Token）
     */
    LoginResponseDTO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    UserDTO getCurrentUser();
}
