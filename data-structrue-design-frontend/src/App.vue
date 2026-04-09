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
    radial-gradient(circle at 10% 15%, rgba(14, 165, 233, 0.2), transparent 34%),
    radial-gradient(circle at 90% 10%, rgba(59, 130, 246, 0.18), transparent 32%),
    linear-gradient(180deg, #eff6ff 0%, #f8fafc 100%);
}

.page-container {
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px;
}

.login-shell {
  background: #0f172a;
}

.login-container {
  max-width: none;
  padding: 0;
}
</style>
