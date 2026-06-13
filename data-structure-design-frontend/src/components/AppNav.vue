<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bowl,
  Calendar,
  Compass,
  Home,
  MapDraw,
  MapRoad,
  Navigation,
  NotebookAndPen,
  Robot,
  Search,
  Shop,
} from '@icon-park/vue-next'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const navItems = [
  { label: '首页', path: '/' },
  { label: '目的地', path: '/destinations' },
  { label: '路线', path: '/routes' },
  { label: '设施', path: '/facilities' },
  { label: '美食', path: '/foods' },
  { label: '日记', path: '/diaries' },
  { label: '行程', path: '/itineraries' },
  { label: '旅行助手', path: '/agent' },
]

const searchActions = [
  { label: '去哪儿', aria: '打开目的地综合推荐', target: '/destinations' },
  { label: '怎么玩', aria: '打开游玩景点设施推荐', target: '/facilities' },
  { label: '吃什么', aria: '打开美食推荐', target: '/foods' },
]

const activePath = computed(() => route.path)

const navIcons = {
  '/': Home,
  '/destinations': MapDraw,
  '/routes': MapRoad,
  '/facilities': Shop,
  '/foods': Bowl,
  '/diaries': NotebookAndPen,
  '/itineraries': Calendar,
  '/agent': Robot,
}

const searchIcons = [Compass, Navigation, Bowl]

const go = (target) => {
  router.push(target)
}

const logout = () => {
  appStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="nav-shell">
    <button class="brand" type="button" aria-label="返回首页" @click="go('/')">
      <span class="brand-mark">T</span>
      <span class="brand-copy">
        <strong>拾迹成行</strong>
        <small>拾迹成行</small>
      </span>
    </button>

    <div class="search-pill" role="group" aria-label="旅行快捷入口">
      <button
        v-for="(item, index) in searchActions"
        :key="item.label"
        class="search-option"
        type="button"
        :aria-label="item.aria"
        @click="go(item.target)"
      >
        <component :is="searchIcons[index]" theme="outline" size="16" fill="currentColor" />
        {{ item.label }}
      </button>
      <button class="search-btn" type="button" aria-label="打开目的地综合推荐" @click="go('/destinations')">
        <Search theme="outline" size="17" fill="currentColor" />
      </button>
    </div>

    <div class="user-actions">
      <button class="route-link" type="button" @click="go('/routes')">路线规划</button>
      <div class="user-chip">
        <span class="user-name">{{ appStore.user.name || '访客' }}</span>
        <button class="logout-btn" type="button" @click="logout">退出</button>
      </div>
    </div>
  </header>

  <nav class="category-bar" aria-label="主导航">
    <button
      v-for="item in navItems"
      :key="item.path"
      type="button"
      class="category-pill"
      :class="{ active: activePath === item.path }"
      :aria-current="activePath === item.path ? 'page' : undefined"
      @click="go(item.path)"
    >
      <component :is="navIcons[item.path]" theme="outline" size="16" fill="currentColor" />
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
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(13, 15, 18, 0.92);
  backdrop-filter: blur(18px);
}

.brand {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
}

.brand-mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #f8fafc;
  color: #111214;
  font-size: 18px;
  font-weight: 900;
}

.brand-copy strong,
.brand-copy small {
  display: block;
}

.brand-copy strong {
  color: #f8fafc;
  font-size: 18px;
  line-height: 1;
  font-weight: 900;
}

.brand-copy small {
  margin-top: 4px;
  color: #a7b0bf;
  font-size: 12px;
  font-weight: 600;
}

.search-pill {
  justify-self: center;
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  padding: 8px 8px 8px 18px;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.22);
  font-size: 14px;
  color: #f8fafc;
}

.search-option {
  min-height: 32px;
  border: 0;
  background: transparent;
  color: #f8fafc;
  font: inherit;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding-right: 10px;
  border-right: 1px solid rgba(255, 255, 255, 0.12);
  white-space: nowrap;
}

.search-option :deep(.i-icon),
.category-pill :deep(.i-icon) {
  transform: translateY(1.5px);
}

.search-option:hover,
.search-option:focus-visible {
  color: #ffffff;
}

.search-option:last-of-type {
  border-right: 0;
  padding-right: 0;
}

.search-btn {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 0;
  background: #ff385c;
  color: #ffffff;
  cursor: pointer;
}

.user-actions {
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 12px;
}

.route-link,
.logout-btn,
.category-pill {
  min-height: 44px;
  cursor: pointer;
}

.route-link {
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #f8fafc;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 800;
}

.route-link:hover,
.route-link:focus-visible {
  background: rgba(255, 255, 255, 0.1);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  padding: 6px 6px 6px 14px;
}

.user-name {
  color: #a7b0bf;
  font-size: 13px;
  font-weight: 700;
}

.logout-btn {
  border: 0;
  border-radius: 999px;
  background: #f8fafc;
  color: #111214;
  padding: 0 14px;
  font-size: 12px;
  font-weight: 800;
}

.category-bar {
  max-width: 1440px;
  margin: 0 auto;
  padding: 12px 24px 6px;
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scrollbar-width: thin;
}

.category-pill {
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #a7b0bf;
  font-size: 14px;
  font-weight: 800;
  padding: 0 2px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.category-pill.active {
  color: #f8fafc;
  border-bottom-color: #ff385c;
}

button:focus-visible {
  outline: 3px solid rgba(255, 56, 92, 0.22);
  outline-offset: 3px;
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

  .brand-copy small,
  .route-link {
    display: none;
  }

  .search-pill {
    width: 100%;
    justify-content: space-between;
  }

  .user-actions {
    width: 100%;
  }

  .user-chip {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
