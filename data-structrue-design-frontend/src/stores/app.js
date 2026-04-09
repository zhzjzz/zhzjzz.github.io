import { defineStore } from 'pinia'

/**
 * 应用级状态仓库。
 * 主要管理登录用户信息与登录态，供路由守卫和导航栏统一使用。
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    token: localStorage.getItem('travel-token') || '',
    user: {
      name: localStorage.getItem('travel-user-name') || '',
      interests: ['校园', '博物馆', '小吃'],
    },
  }),
  getters: {
    // 判断当前是否已登录。
    isLoggedIn: (state) => Boolean(state.token),
  },
  actions: {
    /**
     * 写入登录态并持久化到本地，避免刷新页面后丢失登录状态。
     */
    setLogin({ token, displayName }) {
      this.token = token
      this.user.name = displayName
      localStorage.setItem('travel-token', token)
      localStorage.setItem('travel-user-name', displayName)
    },
    /**
     * 清理登录态，用于用户主动退出登录。
     */
    logout() {
      this.token = ''
      this.user.name = ''
      localStorage.removeItem('travel-token')
      localStorage.removeItem('travel-user-name')
    },
  },
})
