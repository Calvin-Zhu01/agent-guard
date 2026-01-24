/**
 * 标签栏类型定义
 */

export interface TagItem {
  /** 路由路径 */
  path: string
  /** 显示标题 */
  title: string
  /** 路由名称 */
  name?: string
  /** 是否可关闭 */
  closeable: boolean
  /** 完整路由路径用于导航 */
  fullPath?: string
}

export interface TagsState {
  /** 已访问的标签列表 */
  visitedTags: TagItem[]
  /** 当前激活的标签路径 */
  activeTagPath: string
}
