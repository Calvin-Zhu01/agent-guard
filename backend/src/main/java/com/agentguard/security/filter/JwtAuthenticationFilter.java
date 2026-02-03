package com.agentguard.security.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.common.response.Result;
import com.agentguard.security.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * JWT 认证过滤器
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 跳过代理接口的JWT认证（Agent通过API Key认证）
        if (path.startsWith("/proxy/")) {
            return true;
        }
        // 跳过审批状态查询和理由提交接口（Agent SDK轮询使用）
        if (path.matches("/api/v1/approvals/[^/]+/status") ||
            path.matches("/api/v1/approvals/[^/]+/reason")) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StrUtil.isNotBlank(token)) {
            try {
                // 先检查token是否过期
                if (jwtUtil.isTokenExpired(token)) {
                    log.warn("JWT已过期: {}", request.getRequestURI());
                    handleExpiredToken(response);
                    return;
                }

                // 验证token有效性
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);

                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );

                    // 设置认证信息到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT认证成功: userId={}, username={}, role={}", userId, username, role);
                } else {
                    log.warn("JWT验证失败: {}", request.getRequestURI());
                    handleInvalidToken(response);
                    return;
                }
            } catch (ExpiredJwtException e) {
                log.warn("JWT已过期: {}", request.getRequestURI());
                handleExpiredToken(response);
                return;
            } catch (Exception e) {
                log.warn("JWT认证失败: {}", e.getMessage());
                handleInvalidToken(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StrUtil.isNotBlank(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 处理JWT过期的情况
     */
    private void handleExpiredToken(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<?> result = Result.error(ErrorCode.USER_TOKEN_INVALID.getCode(), "Token已过期，请重新登录");
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

    /**
     * 处理JWT无效的情况
     */
    private void handleInvalidToken(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<?> result = Result.error(ErrorCode.USER_TOKEN_INVALID.getCode(), "Token无效，请重新登录");
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }
}
