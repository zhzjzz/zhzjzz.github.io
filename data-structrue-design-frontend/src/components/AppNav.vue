<template>
  <nav class="nav">
    <RouterLink to="/">总览</RouterLink>
    <RouterLink to="/destinations">目的地推荐</RouterLink>
    <RouterLink to="/routes">路线规划</RouterLink>
    <RouterLink to="/diaries">旅游日记</RouterLink>
    <RouterLink to="/itineraries">协作行程</RouterLink>
  </nav>
</template>

<style scoped>
.nav { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.nav a { text-decoration: none; color: #2563eb; font-weight: 600; }
.nav a.router-link-active { color: #1d4ed8; border-bottom: 2px solid #1d4ed8; }
</style>
