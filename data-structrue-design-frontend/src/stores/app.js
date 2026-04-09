import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    user: {
      name: '演示用户',
      interests: ['校园', '博物馆', '小吃'],
    },
  }),
})
