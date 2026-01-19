package com.agentguard.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.security.util.JwtUtil;
import com.agentguard.user.dto.LoginDTO;
import com.agentguard.user.dto.LoginResponseDTO;
import com.agentguard.user.dto.RegisterDTO;
import com.agentguard.user.dto.UserDTO;
import com.agentguard.user.entity.UserDO;
import com.agentguard.user.enums.UserRole;
import com.agentguard.user.enums.UserStatus;
import com.agentguard.user.mapper.UserMapper;
import com.agentguard.user.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类
 *
 * @author zhuhx
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public UserDTO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, dto.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_NAME_DUPLICATE);
        }

        // 创建用户
        UserDO userDO = new UserDO();
        userDO.setUsername(dto.getUsername());
        userDO.setPassword(passwordEncoder.encode(dto.getPassword()));
        userDO.setEmail(dto.getEmail());
        userDO.setRole(UserRole.USER);
        userDO.setStatus(UserStatus.ENABLED);

        userMapper.insert(userDO);

        return toDTO(userDO);
    }

    @Override
    public LoginResponseDTO login(LoginDTO dto) {
        // 查询用户
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, dto.getUsername());
        UserDO userDO = userMapper.selectOne(queryWrapper);

        // 验证用户存在且密码正确
        if (ObjectUtil.isNull(userDO) || !passwordEncoder.matches(dto.getPassword(), userDO.getPassword())) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }

        // 验证用户状态
        if (userDO.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }

        // 生成 Token
        String token = jwtUtil.generateToken(
                userDO.getId(),
                userDO.getUsername(),
                userDO.getRole().getCode()
        );

        return new LoginResponseDTO(token, jwtUtil.getExpiration(), toDTO(userDO));
    }

    @Override
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (ObjectUtil.isNull(authentication) || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.USER_TOKEN_INVALID);
        }

        String userId = authentication.getName();
        UserDO userDO = userMapper.selectById(userId);
        if (ObjectUtil.isNull(userDO)) {
            throw new BusinessException(ErrorCode.USER_TOKEN_INVALID);
        }

        return toDTO(userDO);
    }

    /**
     * 转换为 DTO
     */
    private UserDTO toDTO(UserDO userDO) {
        UserDTO dto = BeanUtil.copyProperties(userDO, UserDTO.class);
        dto.setRole(userDO.getRole().getCode());
        dto.setStatus(userDO.getStatus().getCode());
        return dto;
    }
}
