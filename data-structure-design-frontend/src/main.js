import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './style.css'

/**
 * 应用启动入口：
 * - 注入 Pinia 状态管理；
 * - 注入路由系统；
 * - 注入 Element Plus 组件库用于页面美化。
 */
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
