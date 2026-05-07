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
      interests: (localStorage.getItem('travel-user-interests') || '校园,博物馆,小吃').split(','),
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
    setLogin({ token, displayName, interests }) {
      this.token = token
      this.user.name = displayName
      this.user.interests = interests || ['校园', '博物馆', '小吃']
      localStorage.setItem('travel-token', token)
      localStorage.setItem('travel-user-name', displayName)
      localStorage.setItem('travel-user-interests', this.user.interests.join(','))
    },
    /**
     * 清理登录态，用于用户主动退出登录。
     */
    logout() {
      this.token = ''
      this.user.name = ''
      this.user.interests = ['校园', '博物馆', '小吃']
      localStorage.removeItem('travel-token')
      localStorage.removeItem('travel-user-name')
      localStorage.removeItem('travel-user-interests')
    },
  },
})
