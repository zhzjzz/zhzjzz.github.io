<script setup>
import { onMounted, ref } from 'vue'
import { getTopDestinations, searchDestinations } from '../api/travel'

const keyword = ref('')
const rows = ref([])

const loadTop = async () => {
  const { data } = await getTopDestinations(10)
  rows.value = data
}

const doSearch = async () => {
  const { data } = await searchDestinations(keyword.value)
  rows.value = data
}

onMounted(loadTop)
</script>

<template>
  <section class="card">
    <h2>目的地推荐</h2>
    <div class="toolbar">
      <input v-model="keyword" placeholder="输入关键字（名称/类别）" />
      <button @click="doSearch">检索</button>
      <button @click="loadTop">Top10</button>
    </div>
    <table>
      <thead><tr><th>名称</th><th>场景</th><th>类别</th><th>热度</th><th>评分</th></tr></thead>
      <tbody>
        <tr v-for="item in rows" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ item.sceneType }}</td>
          <td>{{ item.category }}</td>
          <td>{{ item.heat }}</td>
          <td>{{ item.rating }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.card { background: #fff; padding: 16px; border-radius: 8px; }
.toolbar { display: flex; gap: 8px; margin: 12px 0; }
input { padding: 6px 8px; min-width: 260px; }
button { padding: 6px 10px; }
table { width: 100%; border-collapse: collapse; }
th, td { border-bottom: 1px solid #e5e7eb; text-align: left; padding: 8px; }
</style>
