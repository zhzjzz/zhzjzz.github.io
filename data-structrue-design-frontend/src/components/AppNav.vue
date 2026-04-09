<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const navItems = [
  { label: '总览', path: '/' },
  { label: '目的地推荐', path: '/destinations' },
  { label: '路线规划', path: '/routes' },
  { label: '旅游日记', path: '/diaries' },
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
    <div class="brand" @click="go('/')">
      <strong>TravelStay</strong>
      <span>个性化旅游系统</span>
    </div>

    <div class="search-pill" @click="go('/destinations')">
      <span>Anywhere</span>
      <span>Any week</span>
      <span>Add guests</span>
      <button class="search-btn" type="button">⌕</button>
    </div>

    <div class="user-actions">
      <button class="host-link" type="button">Become a Host</button>
      <div class="user-chip">
        <span class="user-name">{{ appStore.user.name || '未登录' }}</span>
        <button class="logout-btn" type="button" @click="logout">退出</button>
      </div>
    </div>
  </header>

  <nav class="category-bar">
    <button
      v-for="item in navItems"
      :key="item.path"
      type="button"
      class="category-pill"
      :class="{ active: activePath === item.path }"
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
  border-bottom: 1px solid #ebebeb;
  background: #ffffff;
}

.brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}

.brand strong {
  color: #ff385c;
  font-size: 22px;
  line-height: 1;
  font-weight: 700;
}

.brand span {
  color: #6a6a6a;
  font-size: 12px;
  font-weight: 500;
}

.search-pill {
  justify-self: center;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  border: 1px solid #ebebeb;
  padding: 8px 8px 8px 16px;
  box-shadow: rgba(0, 0, 0, 0.08) 0px 1px 5px;
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
  color: #6a6a6a;
}

.search-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #ff385c;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
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
  font-weight: 500;
  padding: 8px;
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
  gap: 14px;
  overflow-x: auto;
  scrollbar-width: thin;
}

.category-pill {
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #6a6a6a;
  font-size: 14px;
  font-weight: 600;
  padding: 8px 2px;
  white-space: nowrap;
  cursor: pointer;
}

.category-pill.active {
  color: #222222;
  border-bottom-color: #222222;
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
