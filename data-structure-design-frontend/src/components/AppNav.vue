<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import { Search } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const navItems = [
  { label: '总览', path: '/' },
  { label: '目的地推荐', path: '/destinations' },
  { label: '路线规划', path: '/routes' },
  { label: '场所查询', path: '/facilities' },
  { label: '旅行日记', path: '/diaries' },
  { label: '协作行程', path: '/itineraries' },
]

const activePath = computed(() => route.path)

const go = (path) => {
  router.push(path)
}

const logout = () => {
  appStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="nav-shell">
    <button class="brand" type="button" aria-label="返回总览" @click="go('/')">
      <span class="brand-mark">T</span>
      <span>
        <strong>Travel.AI</strong>
        <small>个性化旅行系统</small>
      </span>
    </button>

    <button class="search-pill" type="button" aria-label="进入目的地推荐" @click="go('/destinations')">
      <span>景区</span>
      <span>校园</span>
      <span>路线</span>
      <span class="search-btn">
        <el-icon><Search /></el-icon>
      </span>
    </button>

    <div class="user-actions">
      <button class="host-link" type="button" @click="go('/routes')">演示路线</button>
      <div class="user-chip">
        <span class="user-name">{{ appStore.user.name || '未登录' }}</span>
        <button class="logout-btn" type="button" @click="logout">退出</button>
      </div>
    </div>
  </header>

  <nav class="category-bar" aria-label="主功能导航">
    <button
      v-for="item in navItems"
      :key="item.path"
      type="button"
      class="category-pill"
      :class="{ active: activePath === item.path }"
      :aria-current="activePath === item.path ? 'page' : undefined"
      @click="go(item.path)"
    >
      {{ item.label }}
    </button>
  </nav>
</template>

<style scoped>
.nav-shell {
  position: sticky;
  top: 0;
  z-index: 20;
  max-width: 1440px;
  margin: 0 auto;
  padding: 16px 24px 12px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 18px;
  align-items: center;
  border-bottom: 1px solid rgba(235, 235, 235, 0.9);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(18px);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff385c, #d90f3f);
  color: #ffffff;
  font-weight: 900;
  box-shadow: rgba(255, 56, 92, 0.26) 0 12px 28px;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  color: #222222;
  font-size: 18px;
  line-height: 1;
  font-weight: 800;
}

.brand small {
  margin-top: 3px;
  color: #6a6a6a;
  font-size: 12px;
}

.search-pill {
  justify-self: center;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  border: 1px solid #ebebeb;
  background: #ffffff;
  padding: 8px 8px 8px 16px;
  box-shadow: rgba(0, 0, 0, 0.08) 0 2px 10px;
  font-size: 14px;
  color: #222222;
  cursor: pointer;
}

.search-pill span {
  padding-right: 10px;
  border-right: 1px solid #ebebeb;
  white-space: nowrap;
}

.search-pill span:last-of-type {
  border-right: none;
  padding-right: 0;
}

.search-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #ff385c;
  color: #ffffff;
  display: grid;
  place-items: center;
}

.user-actions {
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 12px;
}

.host-link {
  border: none;
  background: transparent;
  color: #222222;
  font-weight: 600;
  padding: 8px 10px;
  border-radius: 999px;
  cursor: pointer;
}

.host-link:hover {
  background: #f7f7f7;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #ebebeb;
  border-radius: 999px;
  background: #ffffff;
  padding: 6px 6px 6px 12px;
}

.user-name {
  font-size: 13px;
  color: #6a6a6a;
}

.logout-btn {
  border: none;
  background: #222222;
  color: #ffffff;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.category-bar {
  max-width: 1440px;
  margin: 0 auto;
  padding: 12px 24px 6px;
  display: flex;
  gap: 18px;
  overflow-x: auto;
  scrollbar-width: thin;
}

.category-pill {
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #6a6a6a;
  font-size: 14px;
  font-weight: 700;
  padding: 8px 2px;
  white-space: nowrap;
  cursor: pointer;
}

.category-pill.active {
  color: #111827;
  border-bottom-color: #ff385c;
}

@media (max-width: 1128px) {
  .nav-shell {
    grid-template-columns: 1fr;
  }

  .search-pill,
  .user-actions {
    justify-self: start;
  }
}

@media (max-width: 744px) {
  .nav-shell,
  .category-bar {
    padding-left: 16px;
    padding-right: 16px;
  }

  .search-pill {
    width: 100%;
    justify-content: space-between;
  }

  .host-link {
    display: none;
  }
}
</style>
