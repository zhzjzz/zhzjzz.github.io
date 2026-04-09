<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const activePath = computed(() => route.path)

/**
 * 导航切换：保持界面菜单与路由地址同步。
 */
const handleSelect = (path) => {
  router.push(path)
}

/**
 * 退出登录并回到登录页。
 */
const logout = () => {
  appStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="nav-shell">
    <div class="logo-block">
      <strong>个性化旅游系统</strong>
      <span>{{ appStore.user.name || '未登录' }}</span>
    </div>
    <el-menu
      :default-active="activePath"
      mode="horizontal"
      class="menu"
      @select="handleSelect"
    >
      <el-menu-item index="/">总览</el-menu-item>
      <el-menu-item index="/destinations">目的地推荐</el-menu-item>
      <el-menu-item index="/routes">路线规划</el-menu-item>
      <el-menu-item index="/diaries">旅游日记</el-menu-item>
      <el-menu-item index="/itineraries">协作行程</el-menu-item>
    </el-menu>
    <el-button type="danger" plain @click="logout">退出登录</el-button>
  </header>
</template>

<style scoped>
.nav-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 12px 20px 0;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 14px;
}

.logo-block {
  display: grid;
  gap: 2px;
}

.logo-block strong {
  color: #1e3a8a;
  font-size: 18px;
}

.logo-block span {
  color: #64748b;
  font-size: 12px;
}

.menu {
  min-width: 0;
  border-bottom: none;
  background: rgba(255, 255, 255, 0.76);
  border-radius: 14px;
  padding: 0 10px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

@media (max-width: 980px) {
  .nav-shell {
    grid-template-columns: 1fr;
  }
}
</style>
