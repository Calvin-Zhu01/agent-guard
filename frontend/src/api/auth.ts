/**
 * 认证 API 请求封装
 *
 * @author zhuhx
 */
import request from '@/utils/request'
import type { User, LoginDTO, RegisterDTO, LoginResponse } from '@/types/user'

/**
 * 用户注册
 */
export function register(data: RegisterDTO): Promise<User> {
  return request.post('/auth/register', data)
}

/**
 * 用户登录
 */
export function login(data: LoginDTO): Promise<LoginResponse> {
  return request.post('/auth/login', data)
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser(): Promise<User> {
  return request.get('/auth/me')
}

/**
 * 退出登录
 */
export function logout(): Promise<void> {
  return request.post('/auth/logout')
}
