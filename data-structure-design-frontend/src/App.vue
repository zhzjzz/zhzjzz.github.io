<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppNav from './components/AppNav.vue'

const route = useRoute()
const isLoginPage = computed(() => route.name === 'login')
</script>

<template>
  <main :class="['app-shell', { 'login-shell': isLoginPage }]">
    <AppNav v-if="!isLoginPage" />
    <section :class="['page-container', { 'login-container': isLoginPage }]">
      <RouterView />
    </section>
  </main>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(248, 250, 252, 0.96) 48%, #ffffff 100%);
}

.page-container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 30px 24px 56px;
}

.login-shell {
  background: #ffffff;
}

.login-container {
  max-width: none;
  padding: 0;
}

@media (max-width: 744px) {
  .page-container {
    padding: 20px 16px 32px;
  }
}
</style>
