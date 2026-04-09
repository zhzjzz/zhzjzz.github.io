<script setup>
import AppNav from './components/AppNav.vue'
</script>

<template>
  <main class="container">
    <AppNav />
    <RouterView />
  </main>
</template>

<style scoped>
.container { max-width: 1080px; margin: 0 auto; padding: 20px; }
</style>
