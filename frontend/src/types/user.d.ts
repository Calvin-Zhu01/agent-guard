/**
 * 用户相关类型定义
 *
 * @author zhuhx
 */

/** 用户角色 */
export type UserRole = 'ADMIN' | 'USER'

/** 用户状态 */
export type UserStatus = 0 | 1

/** 用户信息 */
export interface User {
  id: string
  username: string
  email: string
  role: UserRole
  status: UserStatus
  createdAt: string
}

/** 登录请求 */
export interface LoginDTO {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterDTO {
  username: string
  password: string
  email?: string
}

/** 登录响应 */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: User
}
