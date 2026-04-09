<script setup>
import { ref } from 'vue'
import { planRoute } from '../api/travel'

const form = ref({ fromNodeId: 1, toNodeId: 2, strategy: 'time', transport: 'walk' })
const result = ref(null)
const error = ref('')

const submit = async () => {
  error.value = ''
  result.value = null
  try {
    const { data } = await planRoute(form.value)
    result.value = data
  } catch (e) {
    error.value = e.response?.data?.message || '规划失败，请检查节点和后端服务。'
  }
}
</script>

<template>
  <section class="card">
    <h2>路线规划</h2>
    <div class="grid">
      <label>起点节点ID <input v-model.number="form.fromNodeId" type="number" /></label>
      <label>终点节点ID <input v-model.number="form.toNodeId" type="number" /></label>
      <label>策略
        <select v-model="form.strategy">
          <option value="distance">最短距离</option>
          <option value="time">最短时间</option>
        </select>
      </label>
      <label>交通工具
        <select v-model="form.transport">
          <option value="walk">步行</option>
          <option value="bike">自行车</option>
          <option value="shuttle">电瓶车</option>
        </select>
      </label>
    </div>
    <button @click="submit">生成路线</button>
    <p v-if="error" class="error">{{ error }}</p>
    <pre v-if="result">{{ result }}</pre>
  </section>
</template>

<style scoped>
.card { background: #fff; padding: 16px; border-radius: 8px; }
.grid { display: grid; grid-template-columns: repeat(2,minmax(180px,1fr)); gap: 10px; margin-bottom: 10px; }
input,select { width: 100%; margin-top: 4px; padding: 6px; }
button { padding: 6px 10px; }
.error { color: #dc2626; }
</style>
